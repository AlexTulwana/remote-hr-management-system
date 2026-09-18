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

    public static EmployeeRequestSummary from(com.wethinkcode.hrsystem.model.EmployeeRequest r) {
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
                r.getResolvedAt()
        );
    }
}
