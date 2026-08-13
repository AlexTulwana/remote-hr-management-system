package com.wethinkcode.hrsystem.dto;

import java.time.LocalDateTime;

public record HearingScheduledEvent(
        Long hearingId,
        Long employeeId,
        String employeeEmail,
        String employeeName,
        LocalDateTime scheduledAt,
        String meetingLink,
        String conductedByName
) {}