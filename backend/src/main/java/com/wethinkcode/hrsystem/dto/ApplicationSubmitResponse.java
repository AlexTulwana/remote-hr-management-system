package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Application;

import java.time.LocalDateTime;

// Candidate-facing response after a public, unauthenticated submission.
// No email templates, no internal file paths, no nested JobPosting entity.
public record ApplicationSubmitResponse(
        Long id,
        String jobPostingTitle,
        String candidateName,
        LocalDateTime submittedAt,
        String status
) {
    public static ApplicationSubmitResponse from(Application a) {
        return new ApplicationSubmitResponse(
                a.getId(),
                a.getJobPosting() != null ? a.getJobPosting().getTitle() : null,
                a.getCandidateName(),
                a.getSubmittedAt(),
                a.getStatus()
        );
    }
}
