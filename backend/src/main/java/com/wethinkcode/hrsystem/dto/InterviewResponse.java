package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Interview;
import java.time.LocalDateTime;

public record InterviewResponse(
        Long id,
        Long applicationId,
        String candidateName,
        String type,
        LocalDateTime interviewDateTime,
        String location,
        String meetingLink,
        String status,
        String notes
) {
    public static InterviewResponse from(Interview interview) {
        return new InterviewResponse(
                interview.getId(),
                interview.getApplication() != null ? interview.getApplication().getId() : null,
                interview.getApplication() != null ? interview.getApplication().getCandidateName() : null,
                interview.getType(),
                interview.getInterviewDateTime(),
                interview.getLocation(),
                interview.getMeetingLink(),
                interview.getStatus(),
                interview.getNotes()
        );
    }
}
