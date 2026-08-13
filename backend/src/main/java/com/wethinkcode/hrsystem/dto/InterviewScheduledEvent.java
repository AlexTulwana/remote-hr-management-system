package com.wethinkcode.hrsystem.dto;

import java.time.LocalDateTime;

public record InterviewScheduledEvent(
        Long interviewId,
        Long applicationId,
        String candidateEmail,
        String candidateName,
        String jobTitle,
        LocalDateTime scheduledAt,
        String meetingLink,
        String location
) {}