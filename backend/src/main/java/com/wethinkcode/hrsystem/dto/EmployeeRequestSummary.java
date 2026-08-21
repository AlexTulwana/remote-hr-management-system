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

    public static EmployeeRequestSummary from(com.wethinkcode.hrsystem.model.EmployeeRequest r) {
        return new EmployeeRequestSummary(
                r.getId(),
                r.getRequestType(),
                r.getDescription(),
                r.getStatus(),
                r.getSubmittedAt(),
                EmployeeSummary.from(r.getEmployee())
        );
    }
}