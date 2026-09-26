package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.User;

public record MessageRecipientOption(
        Long userId,
        Long employeeId,
        String fullName,
        String position,
        String branchName,
        boolean hasProfilePicture
) {
    public static MessageRecipientOption from(User user) {
        var employee = user.getEmployee();
        return new MessageRecipientOption(
                user.getId(),
                employee.getId(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                employee.getProfilePicturePath() != null
        );
    }
}
