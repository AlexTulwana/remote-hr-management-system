package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;

public record CurrentUserResponse(
        Long userId,
        String username,
        String role,
        Long employeeId,
        String employeeNumber,
        String fullName,
        String position,
        String department,
        String branchName,
        String employmentStatus
) {
    public static CurrentUserResponse from(User user) {
        Employee employee = user.getEmployee();
        return new CurrentUserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole(),
                employee != null ? employee.getId() : null,
                employee != null ? employee.getEmployeeNumber() : null,
                employee != null ? employee.getFullName() : null,
                employee != null ? employee.getPosition() : null,
                employee != null ? employee.getDepartment() : null,
                employee != null && employee.getBranch() != null ? employee.getBranch().getName() : null,
                employee != null ? employee.getEmploymentStatus() : null
        );
    }
}
