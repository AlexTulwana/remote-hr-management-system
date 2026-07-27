package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.JobPosting;
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
import java.util.UUID;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobPostingRepository jobPostingRepository;
    private final String uploadDir = "uploads/applications/";

    public ApplicationService(ApplicationRepository applicationRepository, JobPostingRepository jobPostingRepository) {
        this.applicationRepository = applicationRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    public Application submit(Long jobPostingId, ApplicationRequest request, MultipartFile cv) {
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

        Application application = new Application();
        application.setJobPosting(posting);
        application.setCandidateName(request.getCandidateName());
        application.setCandidateEmail(request.getCandidateEmail());
        application.setCandidatePhone(request.getCandidatePhone());
        application.setCoverLetter(request.getCoverLetter());
        application.setSubmittedAt(LocalDateTime.now());
        application.setStatus("SUBMITTED");

        if (cv != null && !cv.isEmpty()) {
            application.setCvPath(saveFile(cv));
        }

        return applicationRepository.save(application);
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

    private String saveFile(MultipartFile file) {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = Paths.get(uploadDir + filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            return filePath.toString();
        } catch (IOException e) {
            throw new RuntimeException("Failed to store CV file", e);
        }
    }
}