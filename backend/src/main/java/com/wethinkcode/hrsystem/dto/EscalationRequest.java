package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class EscalationRequest {
    private Long aboutEmployeeId; // optional
    private String type; // MANAGER_ESCALATION, EMPLOYEE_COMPLAINT
    private String reason;
}