package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Application;

import java.time.LocalDateTime;
import java.util.List;

// HR/Admin management view - flattens JobPosting to id+title (no nested entity,
// no email templates riding along), drops raw file paths, uses safe document summaries.
public record ApplicationResponse(
        Long id,
        Long jobPostingId,
        String jobPostingTitle,
        String candidateName,
        String candidateEmail,
        String candidatePhone,
        String coverLetter,
        boolean hasCv,
        List<ApplicationDocumentResponse> documents,
        Boolean meetsRequirements,
        String requirementsReason,
        LocalDateTime reviewedAt,
        String outcome,
        String outcomeReason,
        LocalDateTime decidedAt,
        String status,
        LocalDateTime submittedAt
) {
    public static ApplicationResponse from(Application a) {
        List<ApplicationDocumentResponse> docs = a.getDocuments() == null ? List.of() : a.getDocuments().stream()
                .map(ApplicationDocumentResponse::from)
                .toList();
        return new ApplicationResponse(
                a.getId(),
                a.getJobPosting() != null ? a.getJobPosting().getId() : null,
                a.getJobPosting() != null ? a.getJobPosting().getTitle() : null,
                a.getCandidateName(),
                a.getCandidateEmail(),
                a.getCandidatePhone(),
                a.getCoverLetter(),
                a.getCvPath() != null,
                docs,
                a.getMeetsRequirements(),
                a.getRequirementsReason(),
                a.getReviewedAt(),
                a.getOutcome(),
                a.getOutcomeReason(),
                a.getDecidedAt(),
                a.getStatus(),
                a.getSubmittedAt()
        );
    }
}
