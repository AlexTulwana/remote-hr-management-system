package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Hearing;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class HearingSummary {
    private Long id;
    private EmployeeSummary employee;
    private String conductedByName;
    private String caseType;
    private String description;
    private LocalDateTime hearingDateTime;
    private String meetingLink;
    private String status;
    private String outcome;
    private String notes; // only included for HR and Admin

    // Safe default (the employee's own view): hides the HR notes.
    public static HearingSummary from(Hearing h) {
        return build(h, false);
    }

    // For HR and Admin: includes the notes.
    public static HearingSummary forStaff(Hearing h) {
        return build(h, true);
    }

    private static HearingSummary build(Hearing h, boolean includeNotes) {
        String conductedByName = null;
        if (h.getConductedBy() != null) {
            conductedByName = h.getConductedBy().getEmployee() != null
                    && h.getConductedBy().getEmployee().getFullName() != null
                    ? h.getConductedBy().getEmployee().getFullName()
                    : h.getConductedBy().getUsername();
        }
        return new HearingSummary(
                h.getId(),
                h.getEmployee() != null ? EmployeeSummary.from(h.getEmployee()) : null,
                conductedByName,
                h.getCaseType(),
                h.getDescription(),
                h.getHearingDateTime(),
                h.getMeetingLink(),
                h.getStatus(),
                h.getOutcome(),
                includeNotes ? h.getNotes() : null
        );
    }
}
