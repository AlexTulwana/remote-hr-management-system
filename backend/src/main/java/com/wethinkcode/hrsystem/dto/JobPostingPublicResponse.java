package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.JobPosting;

import java.time.LocalDate;
import java.util.List;

// Candidate-facing view - no internal email templates, no posted-by user info
public record JobPostingPublicResponse(
        Long id,
        String title,
        String description,
        String requirements,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxApplications,
        List<DocumentType> requiredDocuments,
        String branchName
) {
    public static JobPostingPublicResponse from(JobPosting p) {
        return new JobPostingPublicResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getRequirements(),
                p.getDepartment(),
                p.getStartDate(),
                p.getEndDate(),
                p.getMaxApplications(),
                p.getRequiredDocuments(),
                p.getBranch() != null ? p.getBranch().getName() : null
        );
    }
}
