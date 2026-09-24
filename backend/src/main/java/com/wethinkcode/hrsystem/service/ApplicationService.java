package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.ApplicationDocument;
import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.JobPosting;

import com.wethinkcode.hrsystem.repository.ApplicationDocumentRepository;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeRequest;
import org.springframework.beans.factory.annotation.Value;


import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeChangedEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final ApplicationDocumentRepository applicationDocumentRepository;
    private final RabbitTemplate rabbitTemplate;
    private final String uploadDir;
    private final String documentUploadDir;

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobPostingRepository jobPostingRepository,
                              ApplicationDocumentRepository applicationDocumentRepository,
                              RabbitTemplate rabbitTemplate,
                              @Value("${application.upload-dir:uploads/applications/}") String uploadDir,
                              @Value("${application.document-upload-dir:uploads/application-documents/}") String documentUploadDir) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
        this.rabbitTemplate = rabbitTemplate;
        this.uploadDir = uploadDir;
        this.documentUploadDir = documentUploadDir;
    }

    public Application submit(Long jobPostingId, ApplicationRequest request, MultipartFile cv,
                              Map<DocumentType, MultipartFile> additionalDocuments) {
        JobPosting posting = jobPostingRepository.findById(jobPostingId)
                .orElseThrow(() -> new RuntimeException("Job posting not found"));

        LocalDate today = LocalDate.now();
        if (today.isBefore(posting.getStartDate())) {
            throw new RuntimeException("Applications are not yet open for this position");
        }
        if (today.isAfter(posting.getEndDate())) {
            throw new RuntimeException("Applications for this position have closed");
        }

        long currentCount = applicationRepository.countByJobPostingId(jobPostingId);
        if (posting.getMaxApplications() != null && currentCount >= posting.getMaxApplications()) {
            throw new RuntimeException("This position has reached its maximum number of applications");
        }

        // Validate required documents are all present before saving anything
        for (DocumentType required : posting.getRequiredDocuments()) {
            MultipartFile provided = additionalDocuments.get(required);
            if (provided == null || provided.isEmpty()) {
                throw new RuntimeException("Missing required document: " + required);
            }
        }

        Application application = new Application();
        application.setJobPosting(posting);
        application.setCandidateName(request.getCandidateName());
        application.setCandidateEmail(request.getCandidateEmail());
        application.setCandidatePhone(request.getCandidatePhone());
        application.setCoverLetter(request.getCoverLetter());
        application.setSubmittedAt(LocalDateTime.now());
        application.setStatus("SUBMITTED");

        if (cv != null && !cv.isEmpty()) {
            application.setCvPath(saveFile(cv, uploadDir, List.of(".pdf", ".doc", ".docx")));
        }

        Application saved = applicationRepository.save(application);

        for (Map.Entry<DocumentType, MultipartFile> entry : additionalDocuments.entrySet()) {
            MultipartFile file = entry.getValue();
            if (file == null || file.isEmpty()) continue;

            String path = saveFile(file, documentUploadDir, List.of(".pdf", ".jpg", ".jpeg", ".png", ".docx"));

            ApplicationDocument doc = new ApplicationDocument();
            doc.setApplication(saved);
            doc.setDocumentType(entry.getKey());
            doc.setFileName(Paths.get(path).getFileName().toString());
            doc.setFilePath(path);
            doc.setUploadedAt(LocalDateTime.now());
            applicationDocumentRepository.save(doc);
        }

        saved.setDocuments(applicationDocumentRepository.findByApplicationId(saved.getId()));
        return saved;
    }

    public List<ApplicationDocument> getDocuments(Long applicationId) {
        return applicationDocumentRepository.findByApplicationId(applicationId);
    }

    public Application updateStatus(Long applicationId, String status) {
        Application application = getById(applicationId);
        application.setStatus(status);
        return applicationRepository.save(application);
    }

    public Application getById(Long id) {
        return applicationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Application not found"));
    }

    public List<Application> getByJobPosting(Long jobPostingId) {
        return applicationRepository.findByJobPostingId(jobPostingId);
    }

    public List<Application> getByStatus(String status) {
        return applicationRepository.findByStatus(status);
    }

    public List<Application> getAll() {
        return applicationRepository.findAll();
    }

    public Application setOutcome(Long applicationId, ApplicationOutcomeRequest request) {
        Application application = getById(applicationId);

        if (request.getMeetsRequirements() != null) {
            application.setMeetsRequirements(request.getMeetsRequirements());
            application.setRequirementsReason(request.getRequirementsReason());
            application.setReviewedAt(LocalDateTime.now());
        }

        if (request.getOutcome() != null) {
            application.setOutcome(request.getOutcome());
            application.setOutcomeReason(request.getOutcomeReason());
            application.setDecidedAt(LocalDateTime.now());
            application.setStatus(request.getOutcome().equals("ACCEPTED") ? "HIRED" : "REJECTED");
        }

        Application saved = applicationRepository.save(application);

        if (request.getOutcome() != null) {
            ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                    saved.getId(),
                    saved.getJobPosting() != null ? saved.getJobPosting().getId() : null,
                    saved.getCandidateEmail(),
                    saved.getCandidateName(),
                    saved.getJobPosting() != null ? saved.getJobPosting().getTitle() : null,
                    saved.getOutcome(),
                    saved.getOutcomeReason()
            );
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "application.outcome.changed", event);
        }

        return saved;
    }

    public Resource downloadCv(Long applicationId) {
        Application application = getById(applicationId);
        if (application.getCvPath() == null) {
            throw new RuntimeException("No CV uploaded for this application");
        }
        return loadResource(application.getCvPath());
    }

    public Resource downloadDocument(Long applicationId, Long documentId) {
        ApplicationDocument doc = applicationDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));
        if (doc.getApplication() == null || !doc.getApplication().getId().equals(applicationId)) {
            throw new RuntimeException("Document not found");
        }
        return loadResource(doc.getFilePath());
    }

    private Resource loadResource(String path) {
        try {
            Path filePath = Paths.get(path);
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            }
            throw new RuntimeException("File not found: " + path);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Error loading file: " + e.getMessage());
        }
    }

    public MediaType resolveContentType(String filename) {
        String lower = filename == null ? "" : filename.toLowerCase();
        if (lower.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (lower.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (lower.endsWith(".doc")) return MediaType.valueOf("application/msword");
        if (lower.endsWith(".docx")) {
            return MediaType.valueOf("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String saveFile(MultipartFile file, String targetDir, List<String> allowedExtensions) {
        try {
            String originalFilename = Paths.get(file.getOriginalFilename()).getFileName().toString();
            String safeFilename = originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

            String extension = "";
            int dotIndex = safeFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = safeFilename.substring(dotIndex).toLowerCase();
            }
            if (!allowedExtensions.contains(extension)) {
                throw new RuntimeException("File type not allowed: " + extension);
            }

            Files.createDirectories(Paths.get(targetDir));
            String filename = UUID.randomUUID() + extension;
            Path filePath = Paths.get(targetDir).resolve(filename).normalize();

            if (!filePath.startsWith(Paths.get(targetDir).normalize())) {
                throw new RuntimeException("Invalid file path");
            }

            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }
}