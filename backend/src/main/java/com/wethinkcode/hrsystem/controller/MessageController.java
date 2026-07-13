package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<Message> send(@RequestBody MessageRequest request) {
        return ResponseEntity.ok(messageService.send(request));
    }

    @GetMapping("/inbox/{userId}")
    public ResponseEntity<List<Message>> getInbox(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getInbox(userId));
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<Message>> getUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getUnread(userId));
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<Message> markAsRead(@PathVariable Long messageId) {
        return ResponseEntity.ok(messageService.markAsRead(messageId));
    }
}