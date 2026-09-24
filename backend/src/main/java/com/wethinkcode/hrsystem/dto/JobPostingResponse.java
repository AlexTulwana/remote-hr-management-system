package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.JobPosting;

import java.time.LocalDate;
import java.util.List;

// HR/Admin management view - includes email templates and who posted it (by name only)
public record JobPostingResponse(
        Long id,
        String title,
        String description,
        String requirements,
        String department,
        LocalDate startDate,
        LocalDate endDate,
        Integer maxApplications,
        List<DocumentType> requiredDocuments,
        String rejectedEmailTemplate,
        String interviewInviteEmailTemplate,
        String acceptedEmailTemplate,
        Long postedById,
        String postedByName,
        Long branchId,
        String branchName
) {
    public static JobPostingResponse from(JobPosting p) {
        var postedBy = p.getPostedBy();
        String postedByName = null;
        if (postedBy != null) {
            var employee = postedBy.getEmployee();
            postedByName = employee != null ? employee.getFullName() : postedBy.getUsername();
        }
        return new JobPostingResponse(
                p.getId(),
                p.getTitle(),
                p.getDescription(),
                p.getRequirements(),
                p.getDepartment(),
                p.getStartDate(),
                p.getEndDate(),
                p.getMaxApplications(),
                p.getRequiredDocuments(),
                p.getRejectedEmailTemplate(),
                p.getInterviewInviteEmailTemplate(),
                p.getAcceptedEmailTemplate(),
                postedBy != null ? postedBy.getId() : null,
                postedByName,
                p.getBranch() != null ? p.getBranch().getId() : null,
                p.getBranch() != null ? p.getBranch().getName() : null
        );
    }
}
