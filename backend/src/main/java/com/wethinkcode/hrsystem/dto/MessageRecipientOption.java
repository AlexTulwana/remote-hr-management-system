package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.User;

public record MessageRecipientOption(
        Long userId,
        String fullName,
        String position,
        String branchName
) {
    public static MessageRecipientOption from(User user) {
        var employee = user.getEmployee();
        return new MessageRecipientOption(
                user.getId(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getBranch() != null ? employee.getBranch().getName() : null
        );
    }
}
