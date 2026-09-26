package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.model.User;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        Long senderEmployeeId,
        boolean senderHasProfilePicture,
        Long recipientId,
        String recipientName,
        Long recipientEmployeeId,
        boolean recipientHasProfilePicture,
        String content,
        LocalDateTime sentAt,
        boolean read,
        LocalDateTime readAt
) {
    private static String nameOf(User user) {
        if (user.getEmployee() != null) return user.getEmployee().getFullName();
        return user.getUsername();
    }

    private static Long employeeIdOf(User user) {
        return user.getEmployee() != null ? user.getEmployee().getId() : null;
    }

    private static boolean hasPictureOf(User user) {
        return user.getEmployee() != null && user.getEmployee().getProfilePicturePath() != null;
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                nameOf(message.getSender()),
                employeeIdOf(message.getSender()),
                hasPictureOf(message.getSender()),
                message.getRecipient().getId(),
                nameOf(message.getRecipient()),
                employeeIdOf(message.getRecipient()),
                hasPictureOf(message.getRecipient()),
                message.getContent(),
                message.getSentAt(),
                message.isRead(),
                message.getReadAt()
        );
    }
}
