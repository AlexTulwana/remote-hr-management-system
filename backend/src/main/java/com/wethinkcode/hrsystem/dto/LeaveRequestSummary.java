package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class LeaveRequestSummary {
    private Long id;
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String status;
    private String rejectionReason;
    private boolean hasAttachment;
    private String decidedByName;
    private LocalDateTime decidedAt;
    private EmployeeSummary employee;

    public static LeaveRequestSummary from(com.wethinkcode.hrsystem.model.LeaveRequest r) {
        return new LeaveRequestSummary(
                r.getId(),
                r.getLeaveType(),
                r.getStartDate(),
                r.getEndDate(),
                r.getReason(),
                r.getStatus(),
                r.getRejectionReason(),
                r.getAttachmentPath() != null,
                r.getDecidedByName(),
                r.getDecidedAt(),
                r.getEmployee() != null ? EmployeeSummary.from(r.getEmployee()) : null
        );
    }
}
