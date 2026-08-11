package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class EmployeeRequestSubmission {
    private Long employeeId;
    private String requestType; // DOCUMENT, EQUIPMENT, SHIFT_CHANGE, REMOTE_WORK, OTHER
    private String description;
}
