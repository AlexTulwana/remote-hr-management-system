package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.MessageRequest;
import com.wethinkcode.hrsystem.model.Message;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.MessageRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private MessageService messageService;

    private User currentUser;
    private User recipient;
    private MessageRequest request;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setId(1L);

        recipient = new User();
        recipient.setId(2L);

        request = new MessageRequest();
        request.setRecipientId(2L);
        request.setContent("Are you free for a quick sync today?");
    }

    // ---- send() ----

    @Test
    void send_usesAuthenticatedUserAsSender_savesMessage() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(recipient));
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LocalDateTime before = LocalDateTime.now();
        Message result = messageService.send(request);
        LocalDateTime after = LocalDateTime.now();

        assertNotNull(result);
        assertEquals(currentUser, result.getSender());
        assertEquals(recipient, result.getRecipient());
        assertEquals("Are you free for a quick sync today?", result.getContent());
        assertFalse(result.isRead());
        assertNotNull(result.getSentAt());
        assertFalse(result.getSentAt().isBefore(before));
        assertFalse(result.getSentAt().isAfter(after));

        verify(messageRepository).save(any(Message.class));
    }

    @Test
    void send_recipientNotFound_throwsRuntimeException() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.send(request));
        assertEquals("Recipient not found", ex.getMessage());

        verify(messageRepository, never()).save(any());
    }

    // ---- getInbox() ----

    @Test
    void getInbox_ownInbox_returnsMessages() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        List<Message> messages = List.of(new Message(), new Message());
        when(messageRepository.findBySenderIdOrRecipientIdOrderBySentAtDesc(1L, 1L))
                .thenReturn(messages);

        List<Message> result = messageService.getInbox(1L);

        assertEquals(2, result.size());
        verify(messageRepository).findBySenderIdOrRecipientIdOrderBySentAtDesc(1L, 1L);
    }

    @Test
    void getInbox_otherUsersInbox_throwsAccessDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(AccessDeniedException.class, () -> messageService.getInbox(99L));

        verify(messageRepository, never()).findBySenderIdOrRecipientIdOrderBySentAtDesc(any(), any());
    }

    // ---- getUnread() ----

    @Test
    void getUnread_ownMessages_returnsUnread() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        List<Message> messages = List.of(new Message());
        when(messageRepository.findByRecipientIdAndIsReadFalse(1L)).thenReturn(messages);

        List<Message> result = messageService.getUnread(1L);

        assertEquals(1, result.size());
        verify(messageRepository).findByRecipientIdAndIsReadFalse(1L);
    }

    @Test
    void getUnread_otherUsersMessages_throwsAccessDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(AccessDeniedException.class, () -> messageService.getUnread(99L));

        verify(messageRepository, never()).findByRecipientIdAndIsReadFalse(any());
    }

    // ---- markAsRead() ----

    @Test
    void markAsRead_ownMessage_setsReadTrueAndSaves() {
        Message message = new Message();
        message.setId(10L);
        message.setRecipient(currentUser);
        message.setRead(false);
        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(messageRepository.save(any(Message.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Message result = messageService.markAsRead(10L);

        assertTrue(result.isRead());
        verify(messageRepository).save(message);
    }

    @Test
    void markAsRead_notRecipient_throwsAccessDenied() {
        Message message = new Message();
        message.setId(10L);
        message.setRecipient(recipient); // recipient id=2, current user id=1
        when(messageRepository.findById(10L)).thenReturn(Optional.of(message));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(AccessDeniedException.class, () -> messageService.markAsRead(10L));

        verify(messageRepository, never()).save(any());
    }

    @Test
    void markAsRead_messageNotFound_throwsRuntimeException() {
        when(messageRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> messageService.markAsRead(99L));
        assertEquals("Message not found", ex.getMessage());

        verify(messageRepository, never()).save(any());
    }
}