package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.dto.MessageResponse;
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
    public ResponseEntity<MessageResponse> send(@RequestBody MessageRequest request) {
        return ResponseEntity.ok(MessageResponse.from(messageService.send(request)));
    }

    @GetMapping("/inbox/{userId}")
    public ResponseEntity<List<MessageResponse>> getInbox(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getInbox(userId).stream().map(MessageResponse::from).toList());
    }

    @GetMapping("/unread/{userId}")
    public ResponseEntity<List<MessageResponse>> getUnread(@PathVariable Long userId) {
        return ResponseEntity.ok(messageService.getUnread(userId).stream().map(MessageResponse::from).toList());
    }

    @PatchMapping("/{messageId}/read")
    public ResponseEntity<MessageResponse> markAsRead(@PathVariable Long messageId) {
        return ResponseEntity.ok(MessageResponse.from(messageService.markAsRead(messageId)));
    }
}
