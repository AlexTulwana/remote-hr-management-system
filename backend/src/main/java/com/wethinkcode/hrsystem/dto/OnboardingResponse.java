package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Onboarding;

import java.time.LocalDate;
import java.time.LocalDateTime;

// HR/Admin view - flat employee summary and performer username only. No nested
// Employee/User entities, so salary, banking, ID number and password hash never leave the server.
public record OnboardingResponse(
        Long id,
        Long employeeId,
        String employeeNumber,
        String employeeName,
        String position,
        String department,
        String branchName,
        String employmentStatus,
        String performedBy,
        LocalDate startDate,
        String status,
        String notes,
        LocalDateTime createdAt
) {
    public static OnboardingResponse from(Onboarding o) {
        Employee e = o.getEmployee();
        return new OnboardingResponse(
                o.getId(),
                e != null ? e.getId() : null,
                e != null ? e.getEmployeeNumber() : null,
                e != null ? e.getFullName() : null,
                e != null ? e.getPosition() : null,
                e != null ? e.getDepartment() : null,
                e != null && e.getBranch() != null ? e.getBranch().getName() : null,
                e != null ? e.getEmploymentStatus() : null,
                o.getPerformedBy() != null ? o.getPerformedBy().getUsername() : null,
                o.getStartDate(),
                o.getStatus(),
                o.getNotes(),
                o.getCreatedAt()
        );
    }
}
