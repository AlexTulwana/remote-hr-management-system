package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.JobPosting;

import java.time.LocalDateTime;

public record OnboardingCandidateResponse(
        Long applicationId,
        String candidateName,
        String candidateEmail,
        String candidatePhone,
        String jobPostingTitle,
        String department,
        Long branchId,
        String branchName,
        LocalDateTime decidedAt
) {
    public static OnboardingCandidateResponse from(Application a) {
        if (a == null) {
            return null;
        }
        JobPosting posting = a.getJobPosting();
        Branch branch = posting != null ? posting.getBranch() : null;
        return new OnboardingCandidateResponse(
                a.getId(),
                a.getCandidateName(),
                a.getCandidateEmail(),
                a.getCandidatePhone(),
                posting != null ? posting.getTitle() : null,
                posting != null ? posting.getDepartment() : null,
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null,
                a.getDecidedAt()
        );
    }
}
