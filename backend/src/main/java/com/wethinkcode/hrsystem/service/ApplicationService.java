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

import java.io.IOException;
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
    private final String uploadDir = "uploads/applications/";
    private final String documentUploadDir = "uploads/application-documents/";

    public ApplicationService(ApplicationRepository applicationRepository,
                              JobPostingRepository jobPostingRepository,
                              ApplicationDocumentRepository applicationDocumentRepository) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
        this.applicationDocumentRepository = applicationDocumentRepository;
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

        return applicationRepository.findById(saved.getId())
                .orElseThrow(() -> new RuntimeException("Application not found after save"));
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