package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class EmployeeDetail {
    private Long id;
    private String employeeNumber;
    private String fullName;
    private String position;
    private String department;
    private String qualifications;
    private String contactDetails;
    private String email;
    private LocalDate employmentDate;
    private boolean active;
    private String employmentStatus;
    private LocalDate resignationDate;
    private LocalDate terminationDate;
    private Double salary;
    private String payGrade;
    private String branchName;
    private String reportsToName;

    public static EmployeeDetail from(com.wethinkcode.hrsystem.model.Employee e) {
        return new EmployeeDetail(
                e.getId(),
                e.getEmployeeNumber(),
                e.getFullName(),
                e.getPosition(),
                e.getDepartment(),
                e.getQualifications(),
                e.getContactDetails(),
                e.getEmail(),
                e.getEmploymentDate(),
                e.isActive(),
                e.getEmploymentStatus(),
                e.getResignationDate(),
                e.getTerminationDate(),
                e.getSalary(),
                e.getPayGrade(),
                e.getBranch() != null ? e.getBranch().getName() : null,
                e.getReportsTo() != null ? e.getReportsTo().getFullName() : null
        );
    }
}