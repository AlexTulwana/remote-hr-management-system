package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Escalation;

import java.time.LocalDateTime;

public record EscalationResponse(
        Long id,
        String reporterName,
        Long aboutEmployeeId,
        String aboutEmployeeName,
        String type,
        String reason,
        LocalDateTime submittedAt,
        String status,
        Long linkedHearingId
) {
    private static String reporterNameOf(Escalation e) {
        if (e.getReporter() == null) return null;
        var employee = e.getReporter().getEmployee();
        return employee != null ? employee.getFullName() : e.getReporter().getUsername();
    }

    public static EscalationResponse from(Escalation e) {
        return new EscalationResponse(
                e.getId(),
                reporterNameOf(e),
                e.getAboutEmployee() != null ? e.getAboutEmployee().getId() : null,
                e.getAboutEmployee() != null ? e.getAboutEmployee().getFullName() : null,
                e.getType(),
                e.getReason(),
                e.getSubmittedAt(),
                e.getStatus(),
                e.getLinkedHearing() != null ? e.getLinkedHearing().getId() : null
        );
    }
}
