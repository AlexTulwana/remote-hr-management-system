package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.MessageRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository,
                          CurrentUserService currentUserService) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public Message send(MessageRequest request) {
        User sender = currentUserService.getCurrentUser();
        User recipient = userRepository.findById(request.getRecipientId())
                .orElseThrow(() -> new RuntimeException("Recipient not found"));

        if (!currentUserService.isHrOrAdmin() && !isAllowedRecipient(recipient)) {
            throw new AccessDeniedException("You can only message HR, Admin and people in your own branch");
        }

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        return messageRepository.save(message);
    }

    private boolean isAllowedRecipient(User recipient) {
        if ("HR".equals(recipient.getRole()) || "ADMIN".equals(recipient.getRole())) {
            return true;
        }
        Long callerBranchId = currentUserService.getCurrentBranchId();
        Long recipientBranchId = recipient.getEmployee() != null && recipient.getEmployee().getBranch() != null
                ? recipient.getEmployee().getBranch().getId() : null;
        return callerBranchId != null && callerBranchId.equals(recipientBranchId);
    }

    public List<User> lookupRecipients(String query, Long branchId) {
        boolean isHrOrAdmin = currentUserService.isHrOrAdmin();
        Long effectiveBranchId = isHrOrAdmin ? branchId : null;
        String trimmed = query == null ? "" : query.trim();
        String effectiveQuery = trimmed.length() < 2 ? null : trimmed;
        if (effectiveQuery == null && effectiveBranchId == null) {
            return List.of();
        }
        Long currentUserId = currentUserService.getCurrentUser().getId();
        return userRepository.searchRecipients(effectiveQuery, effectiveBranchId, currentUserId)
                .stream()
                .filter(u -> isHrOrAdmin || isAllowedRecipient(u))
                .toList();
    }

    public List<Message> getInbox(Long userId) {
        if (!currentUserService.getCurrentUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to view this inbox");
        }
        return messageRepository.findBySenderIdOrRecipientIdOrderBySentAtDesc(userId, userId);
    }

    public List<Message> getUnread(Long userId) {
        if (!currentUserService.getCurrentUser().getId().equals(userId)) {
            throw new AccessDeniedException("You are not authorized to view these messages");
        }
        return messageRepository.findByRecipientIdAndIsReadFalse(userId);
    }

    public Message markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        if (!currentUserService.getCurrentUser().getId().equals(message.getRecipient().getId())) {
            throw new AccessDeniedException("You are not authorized to update this message");
        }
        if (!message.isRead()) {
            message.setRead(true);
            message.setReadAt(LocalDateTime.now());
        }
        return messageRepository.save(message);
    }
}