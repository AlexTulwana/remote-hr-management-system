package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeDirectoryEntry {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String position;
    private String department;
    private String employmentStatus;
    private String branchName;
    private String contactDetails;
    private String reportsToName; // null if no manager set

    public static EmployeeDirectoryEntry from(com.wethinkcode.hrsystem.model.Employee e) {
        return new EmployeeDirectoryEntry(
                e.getId(),
                e.getEmployeeNumber(),
                e.getFullName(),
                e.getPosition(),
                e.getDepartment(),
                e.getEmploymentStatus(),
                e.getBranch() != null ? e.getBranch().getName() : null,
                e.getContactDetails(),
                e.getReportsTo() != null ? e.getReportsTo().getFullName() : null
        );
    }
}