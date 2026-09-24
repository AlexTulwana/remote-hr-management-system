package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;

public record ManagerOptionResponse(
        Long employeeId,
        String employeeNumber,
        String fullName,
        String position,
        String role,
        Long branchId,
        String branchName
) {
    public static ManagerOptionResponse from(User user) {
        if (user == null || user.getEmployee() == null) {
            return null;
        }
        Employee e = user.getEmployee();
        Branch branch = e.getBranch();
        return new ManagerOptionResponse(
                e.getId(),
                e.getEmployeeNumber(),
                e.getFullName(),
                e.getPosition(),
                user.getRole(),
                branch != null ? branch.getId() : null,
                branch != null ? branch.getName() : null
        );
    }
}
