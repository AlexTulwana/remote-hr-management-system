package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ApplicationOutcomeChangedEvent;
import com.wethinkcode.hrsystem.dto.HearingScheduledEvent;
import com.wethinkcode.hrsystem.dto.InterviewScheduledEvent;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.repository.JobPostingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationConsumerTest {

    @Mock private JavaMailSender mailSender;
    @Mock private JobPostingRepository jobPostingRepository;

    private NotificationConsumer notificationConsumer;

    @BeforeEach
    void setUp() {
        notificationConsumer = new NotificationConsumer(mailSender, jobPostingRepository);
    }

    // ---------- handleHearing() ----------

    @Test
    void handleHearing_noEmail_skipsSend() {
        HearingScheduledEvent event = new HearingScheduledEvent(
                1L, 1L, null, "Emma Employee", LocalDateTime.now(), null, "Manager Mike");

        notificationConsumer.handleHearing(event);

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleHearing_validEmail_sendsMessage() {
        HearingScheduledEvent event = new HearingScheduledEvent(
                1L, 1L, "emma@example.com", "Emma Employee",
                LocalDateTime.of(2026, 2, 1, 10, 0), "https://meet.example.com", "Manager Mike");

        notificationConsumer.handleHearing(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("emma@example.com", message.getTo()[0]);
        assertEquals("Hearing Scheduled", message.getSubject());
        assertTrue(message.getText().contains("Manager Mike"));
        assertTrue(message.getText().contains("https://meet.example.com"));
    }

    @Test
    void handleHearing_mailSenderThrows_doesNotPropagateException() {
        HearingScheduledEvent event = new HearingScheduledEvent(
                1L, 1L, "emma@example.com", "Emma Employee", LocalDateTime.now(), null, "Manager Mike");

        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> notificationConsumer.handleHearing(event));
    }

    // ---------- handleInterview() ----------

    @Test
    void handleInterview_noEmail_skipsSend() {
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                1L, 1L, 1L, null, "Jane Candidate", "Developer", LocalDateTime.now(), null, "Room 1");

        notificationConsumer.handleInterview(event);

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleInterview_noTemplate_usesDefaultBody() {
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                1L, 1L, 1L, "jane@example.com", "Jane Candidate", "Developer",
                LocalDateTime.of(2026, 2, 1, 10, 0), null, "Room 1");

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.empty());

        notificationConsumer.handleInterview(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("Location: Room 1"));
    }

    @Test
    void handleInterview_withTemplate_appliesPlaceholders() {
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                1L, 1L, 1L, "jane@example.com", "Jane Candidate", "Developer",
                LocalDateTime.now(), "https://meet.example.com", null);

        JobPosting posting = new JobPosting();
        posting.setInterviewInviteEmailTemplate("Hi {candidateName}, interview for {jobTitle} confirmed.");
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(posting));

        notificationConsumer.handleInterview(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("Hi Jane Candidate, interview for Developer confirmed.", captor.getValue().getText());
    }

    @Test
    void handleInterview_nullJobPostingId_usesDefaultBodyWithoutLookup() {
        InterviewScheduledEvent event = new InterviewScheduledEvent(
                1L, 1L, null, "jane@example.com", "Jane Candidate", null,
                LocalDateTime.now(), "https://meet.example.com", null);

        notificationConsumer.handleInterview(event);

        verifyNoInteractions(jobPostingRepository);
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    // ---------- handleApplicationOutcomeChanged() ----------

    @Test
    void handleOutcome_noEmail_skipsSend() {
        ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                1L, 1L, null, "Jane Candidate", "Developer", "ACCEPTED", "Great fit");

        notificationConsumer.handleApplicationOutcomeChanged(event);

        verifyNoInteractions(mailSender);
    }

    @Test
    void handleOutcome_accepted_noTemplate_usesDefaultAcceptedBody() {
        ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                1L, 1L, "jane@example.com", "Jane Candidate", "Developer", "ACCEPTED", "Great fit");

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.empty());

        notificationConsumer.handleApplicationOutcomeChanged(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("Congratulations"));
    }

    @Test
    void handleOutcome_rejected_noTemplate_usesDefaultRejectedBody() {
        ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                1L, 1L, "jane@example.com", "Jane Candidate", "Developer", "REJECTED", "Not enough experience");

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.empty());

        notificationConsumer.handleApplicationOutcomeChanged(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertTrue(captor.getValue().getText().contains("we have decided not to proceed"));
    }

    @Test
    void handleOutcome_accepted_withTemplate_appliesPlaceholders() {
        ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                1L, 1L, "jane@example.com", "Jane Candidate", "Developer", "ACCEPTED", "Great fit");

        JobPosting posting = new JobPosting();
        posting.setAcceptedEmailTemplate("Congrats {candidateName}, you got the {jobTitle} role!");
        when(jobPostingRepository.findById(1L)).thenReturn(Optional.of(posting));

        notificationConsumer.handleApplicationOutcomeChanged(event);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertEquals("Congrats Jane Candidate, you got the Developer role!", captor.getValue().getText());
    }

    @Test
    void handleOutcome_mailSenderThrows_doesNotPropagateException() {
        ApplicationOutcomeChangedEvent event = new ApplicationOutcomeChangedEvent(
                1L, 1L, "jane@example.com", "Jane Candidate", "Developer", "ACCEPTED", "Great fit");

        when(jobPostingRepository.findById(1L)).thenReturn(Optional.empty());
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(SimpleMailMessage.class));

        assertDoesNotThrow(() -> notificationConsumer.handleApplicationOutcomeChanged(event));
    }
}