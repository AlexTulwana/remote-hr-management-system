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

        Message message = new Message();
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(request.getContent());
        message.setSentAt(LocalDateTime.now());
        message.setRead(false);

        return messageRepository.save(message);
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
        message.setRead(true);
        return messageRepository.save(message);
    }
}