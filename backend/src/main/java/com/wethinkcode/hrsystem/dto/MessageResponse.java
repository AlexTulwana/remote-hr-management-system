package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Message;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long senderId,
        String senderName,
        Long recipientId,
        String recipientName,
        String content,
        LocalDateTime sentAt,
        boolean read
) {
    private static String nameOf(com.wethinkcode.hrsystem.model.User user) {
        if (user.getEmployee() != null) return user.getEmployee().getFullName();
        return user.getUsername();
    }

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getSender().getId(),
                nameOf(message.getSender()),
                message.getRecipient().getId(),
                nameOf(message.getRecipient()),
                message.getContent(),
                message.getSentAt(),
                message.isRead()
        );
    }
}
