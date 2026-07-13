package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.MessageRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;

    public MessageService(MessageRepository messageRepository, UserRepository userRepository) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
    }

    public Message send(MessageRequest request) {
        User sender = userRepository.findById(request.getSenderId())
                .orElseThrow(() -> new RuntimeException("Sender not found"));
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
        return messageRepository.findBySenderIdOrRecipientIdOrderBySentAtDesc(userId, userId);
    }

    public List<Message> getUnread(Long userId) {
        return messageRepository.findByRecipientIdAndIsReadFalse(userId);
    }

    public Message markAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new RuntimeException("Message not found"));
        message.setRead(true);
        return messageRepository.save(message);
    }
}