package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class EmployeeRequest {
    private String employeeNumber;
    private String fullName;
    private String position;
    private String department;
    private String qualifications;
    private String contactDetails;
    private LocalDate employmentDate;
    private Long branchId;
    private Long reportsToId;
}