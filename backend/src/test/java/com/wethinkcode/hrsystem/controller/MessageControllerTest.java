package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.MessageService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageService messageService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static final String SEND_BODY = """
            {
              "recipientId": 2,
              "content": "Are you free for a quick sync today?"
            }
            """;

    private User buildUser(Long id, String username, String fullName) {
        Employee employee = null;
        if (fullName != null) {
            employee = new Employee();
            employee.setFullName(fullName);
        }
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole("EMPLOYEE");
        user.setEmployee(employee);
        return user;
    }

    private Message buildMessage(Long id, User sender, User recipient, String content, boolean read) {
        Message message = new Message();
        message.setId(id);
        message.setSender(sender);
        message.setRecipient(recipient);
        message.setContent(content);
        message.setSentAt(LocalDateTime.now());
        message.setRead(read);
        return message;
    }

    // ---- send() ----

    @Test
    void send_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/messages")
                        .contentType("application/json")
                        .content(SEND_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void send_authenticatedUser_isAllowed() throws Exception {
        User sender = buildUser(1L, "sender1", "Sam Sender");
        User recipient = buildUser(2L, "recipient1", "Rae Recipient");
        Message message = buildMessage(10L, sender, recipient, "Are you free for a quick sync today?", false);

        when(messageService.send(any(MessageRequest.class))).thenReturn(message);

        mockMvc.perform(post("/api/messages")
                        .contentType("application/json")
                        .content(SEND_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderName").value("Sam Sender"))
                .andExpect(jsonPath("$.recipientName").value("Rae Recipient"))
                .andExpect(jsonPath("$.content").value("Are you free for a quick sync today?"));
    }

    // ---- getInbox() ----

    @Test
    void getInbox_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/messages/inbox/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getInbox_ownInbox_isAllowed() throws Exception {
        User sender = buildUser(2L, "sender1", "Sam Sender");
        User recipient = buildUser(1L, "recipient1", "Rae Recipient");
        Message message = buildMessage(10L, sender, recipient, "Hi there", false);

        when(messageService.getInbox(1L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages/inbox/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].senderName").value("Sam Sender"))
                .andExpect(jsonPath("$[0].recipientName").value("Rae Recipient"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getInbox_otherUsersInbox_deniedByService() throws Exception {
        when(messageService.getInbox(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to view this inbox"));

        mockMvc.perform(get("/api/messages/inbox/99"))
                .andExpect(status().isForbidden());
    }

    // ---- getUnread() ----

    @Test
    void getUnread_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/messages/unread/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getUnread_ownMessages_isAllowed() throws Exception {
        User sender = buildUser(2L, "sender1", "Sam Sender");
        User recipient = buildUser(1L, "recipient1", "Rae Recipient");
        Message message = buildMessage(10L, sender, recipient, "Unread message", false);

        when(messageService.getUnread(1L)).thenReturn(List.of(message));

        mockMvc.perform(get("/api/messages/unread/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].read").value(false));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getUnread_otherUsersMessages_deniedByService() throws Exception {
        when(messageService.getUnread(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to view these messages"));

        mockMvc.perform(get("/api/messages/unread/99"))
                .andExpect(status().isForbidden());
    }

    // ---- markAsRead() ----

    @Test
    void markAsRead_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(patch("/api/messages/10/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void markAsRead_ownMessage_isAllowed() throws Exception {
        User sender = buildUser(2L, "sender1", "Sam Sender");
        User recipient = buildUser(1L, "recipient1", "Rae Recipient");
        Message message = buildMessage(10L, sender, recipient, "Read this", true);

        when(messageService.markAsRead(10L)).thenReturn(message);

        mockMvc.perform(patch("/api/messages/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void markAsRead_notRecipient_deniedByService() throws Exception {
        when(messageService.markAsRead(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to mark this message as read"));

        mockMvc.perform(patch("/api/messages/10/read"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void markAsRead_returnsReadTimestamp() throws Exception {
        User sender = buildUser(2L, "sender1", "Sam Sender");
        User recipient = buildUser(1L, "recipient1", "Rae Recipient");
        Message message = buildMessage(10L, sender, recipient, "Read this", true);
        message.setReadAt(LocalDateTime.of(2026, 9, 20, 14, 32));

        when(messageService.markAsRead(10L)).thenReturn(message);

        mockMvc.perform(patch("/api/messages/10/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.readAt").exists());
    }
}
