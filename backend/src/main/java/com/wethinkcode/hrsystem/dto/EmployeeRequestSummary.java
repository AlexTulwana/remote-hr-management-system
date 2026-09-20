package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.RequestType;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class EmployeeRequestSummary {
    private Long id;
    private RequestType requestType;
    private String description;
    private String status;
    private LocalDateTime submittedAt;
    private EmployeeSummary employee;
    private String handledByName;
    private String managerComment;
    private String hrComment;
    private LocalDateTime resolvedAt;
    private String escalationComment; // only included for managers, HR and Admin

    // Safe default: hides the escalation comment (use for the employee's own view).
    public static EmployeeRequestSummary from(com.wethinkcode.hrsystem.model.EmployeeRequest r) {
        return build(r, false);
    }

    // For managers, HR and Admin: includes the escalation comment.
    public static EmployeeRequestSummary forStaff(com.wethinkcode.hrsystem.model.EmployeeRequest r) {
        return build(r, true);
    }

    private static EmployeeRequestSummary build(com.wethinkcode.hrsystem.model.EmployeeRequest r,
                                                boolean includeEscalationComment) {
        return new EmployeeRequestSummary(
                r.getId(),
                r.getRequestType(),
                r.getDescription(),
                r.getStatus(),
                r.getSubmittedAt(),
                r.getEmployee() != null ? EmployeeSummary.from(r.getEmployee()) : null,
                r.getHandledBy() != null ? r.getHandledBy().getUsername() : null,
                r.getManagerComment(),
                r.getHrComment(),
                r.getResolvedAt(),
                includeEscalationComment ? r.getEscalationComment() : null
        );
    }
}
