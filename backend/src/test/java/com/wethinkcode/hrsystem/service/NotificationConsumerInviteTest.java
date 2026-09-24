package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeInviteEvent;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerInviteTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private JobPostingRepository jobPostingRepository;

    @InjectMocks
    private NotificationConsumer consumer;

    private EmployeeInviteEvent event(String email) {
        return new EmployeeInviteEvent(email, "New Hire", "EMPLOYEE", "abc-123",
                LocalDateTime.of(2026, 10, 8, 9, 0));
    }

    @Test
    void handleEmployeeInvite_sendsEmailWithSetPasswordLink() {
        ReflectionTestUtils.setField(consumer, "frontendBaseUrl", "https://hr.example.com");

        consumer.handleEmployeeInvite(event("new.hire@example.com"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage sent = captor.getValue();

        assertEquals("new.hire@example.com", sent.getTo()[0]);
        assertEquals("Welcome - set up your HR account", sent.getSubject());
        assertTrue(sent.getText().contains("https://hr.example.com/set-password?token=abc-123"));
        assertTrue(sent.getText().contains("Username: new.hire@example.com"));
        assertTrue(sent.getText().contains("Dear New Hire"));
    }

    @Test
    void handleEmployeeInvite_usesLocalhostDefaultWhenNotConfigured() {
        consumer.handleEmployeeInvite(event("new.hire@example.com"));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        assertTrue(captor.getValue().getText().contains("http://localhost:5173/set-password?token=abc-123"));
    }

    @Test
    void handleEmployeeInvite_blankEmail_sendsNothing() {
        consumer.handleEmployeeInvite(event("  "));

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleEmployeeInvite_mailFailure_isSwallowed() {
        doThrow(new RuntimeException("smtp down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> consumer.handleEmployeeInvite(event("new.hire@example.com")));
    }
}
