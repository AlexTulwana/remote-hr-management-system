package com.wethinkcode.hrsystem.dto;

public record ApplicationOutcomeChangedEvent(
        Long applicationId,
        Long jobPostingId,
        String candidateEmail,
        String candidateName,
        String jobTitle,
        String outcome,       // ACCEPTED or REJECTED
        String outcomeReason
) {}