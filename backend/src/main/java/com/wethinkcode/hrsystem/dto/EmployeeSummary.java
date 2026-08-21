package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeSummary {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String position;
    private String department;
    private String employmentStatus;
    private String branchName;

    public static EmployeeSummary from(com.wethinkcode.hrsystem.model.Employee employee) {
        return new EmployeeSummary(
                employee.getId(),
                employee.getEmployeeNumber(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getDepartment(),
                employee.getEmploymentStatus(),
                employee.getBranch() != null ? employee.getBranch().getName() : null
        );
    }
}