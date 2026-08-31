package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.dto.InterviewRequest;
import com.wethinkcode.hrsystem.dto.InterviewScheduledEvent;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.Interview;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import com.wethinkcode.hrsystem.repository.InterviewRepository;
import com.wethinkcode.hrsystem.service.meeting.MeetingDetails;
import com.wethinkcode.hrsystem.service.meeting.MeetingProvider;
import com.wethinkcode.hrsystem.service.meeting.MeetingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class InterviewServiceTest {

    @Mock private InterviewRepository interviewRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private MeetingProvider meetingProvider;
    @Mock private RabbitTemplate rabbitTemplate;

    private Application application;
    private InterviewRequest request;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        JobPosting posting = new JobPosting();
        posting.setId(1L);
        posting.setTitle("Backend Developer");

        application = new Application();
        application.setId(10L);
        application.setJobPosting(posting);
        application.setCandidateName("Jane Candidate");
        application.setCandidateEmail("jane@example.com");

        request = new InterviewRequest();
        request.setApplicationId(10L);
        request.setInterviewDateTime(LocalDateTime.of(2026, 2, 1, 10, 0));
    }

    private InterviewService serviceWithProvider(Optional<MeetingProvider> provider) {
        return new InterviewService(interviewRepository, applicationRepository, provider, rabbitTemplate);
    }

    // ---------- schedule() - in person ----------

    @Test
    void schedule_applicationNotFound_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(applicationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.schedule(request));
    }

    @Test
    void schedule_alreadyScheduled_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));

        Interview existing = new Interview();
        existing.setStatus("SCHEDULED");
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.schedule(request));
        assertTrue(ex.getMessage().contains("already scheduled"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void schedule_previousCancelledInterview_doesNotBlockNewOne() {
        InterviewService service = serviceWithProvider(Optional.empty());
        request.setType("IN_PERSON");
        request.setLocation("Room 1");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        Interview cancelled = new Interview();
        cancelled.setStatus("CANCELLED");
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of(cancelled));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.schedule(request);

        assertEquals("SCHEDULED", result.getStatus());
    }

    @Test
    void schedule_inPerson_setsLocationAndUpdatesApplicationStatus() {
        InterviewService service = serviceWithProvider(Optional.empty());
        request.setType("IN_PERSON");
        request.setLocation("Room 5");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of());
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.schedule(request);

        assertEquals("Room 5", result.getLocation());
        assertNull(result.getMeetingLink());
        assertEquals("INTERVIEW_SCHEDULED", application.getStatus());
        verify(applicationRepository).save(application);

        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("interview.scheduled"),
                any(InterviewScheduledEvent.class));
    }

    // ---------- schedule() - online, manual link ----------

    @Test
    void schedule_online_manualLinkProvided_usesItDirectly() {
        InterviewService service = serviceWithProvider(Optional.empty());
        request.setType("ONLINE");
        request.setMeetingLink("https://manual-link.example.com");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of());
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.schedule(request);

        assertEquals("https://manual-link.example.com", result.getMeetingLink());
        verifyNoInteractions(meetingProvider);
    }

    // ---------- schedule() - online, provider-generated link ----------

    @Test
    void schedule_online_noLinkNoProvider_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());
        request.setType("ONLINE");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.schedule(request));
        assertTrue(ex.getMessage().contains("no meeting provider configured"));
        verify(interviewRepository, never()).save(any());
    }

    @Test
    void schedule_online_providerSucceeds_usesGeneratedLink() {
        InterviewService service = serviceWithProvider(Optional.of(meetingProvider));
        request.setType("ONLINE");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of());
        when(meetingProvider.createMeeting(any(MeetingDetails.class)))
                .thenReturn(new MeetingResult("https://meet.example.com/xyz", "meet-id", true, null));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.schedule(request);

        assertEquals("https://meet.example.com/xyz", result.getMeetingLink());
    }

    @Test
    void schedule_online_providerFails_throwsException() {
        InterviewService service = serviceWithProvider(Optional.of(meetingProvider));
        request.setType("ONLINE");

        when(applicationRepository.findById(10L)).thenReturn(Optional.of(application));
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of());
        when(meetingProvider.createMeeting(any(MeetingDetails.class)))
                .thenReturn(new MeetingResult(null, null, false, "Provider quota exceeded"));
        when(meetingProvider.getProviderName()).thenReturn("GoogleMeet");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.schedule(request));
        assertTrue(ex.getMessage().contains("GoogleMeet"));
        assertTrue(ex.getMessage().contains("Provider quota exceeded"));
        verify(interviewRepository, never()).save(any());
    }

    // ---------- updateStatus() ----------

    @Test
    void updateStatus_invalidStatus_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.updateStatus(1L, "BOGUS", "notes"));
        assertTrue(ex.getMessage().contains("Invalid status"));
        verifyNoInteractions(interviewRepository);
    }

    @Test
    void updateStatus_notFound_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(interviewRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.updateStatus(1L, "COMPLETED", "notes"));
    }

    @Test
    void updateStatus_success() {
        InterviewService service = serviceWithProvider(Optional.empty());
        Interview interview = new Interview();
        interview.setId(1L);
        when(interviewRepository.findById(1L)).thenReturn(Optional.of(interview));
        when(interviewRepository.save(any(Interview.class))).thenAnswer(inv -> inv.getArgument(0));

        Interview result = service.updateStatus(1L, "COMPLETED", "went well");

        assertEquals("COMPLETED", result.getStatus());
        assertEquals("went well", result.getNotes());
    }

    // ---------- getById() / getByApplication() / getAll() ----------

    @Test
    void getById_found_returnsInterview() {
        InterviewService service = serviceWithProvider(Optional.empty());
        Interview interview = new Interview();
        interview.setId(5L);
        when(interviewRepository.findById(5L)).thenReturn(Optional.of(interview));

        assertEquals(interview, service.getById(5L));
    }

    @Test
    void getById_notFound_throwsException() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(interviewRepository.findById(9L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.getById(9L));
    }

    @Test
    void getByApplication_delegatesToRepository() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(interviewRepository.findByApplicationId(10L)).thenReturn(List.of(new Interview()));

        assertEquals(1, service.getByApplication(10L).size());
    }

    @Test
    void getAll_delegatesToRepository() {
        InterviewService service = serviceWithProvider(Optional.empty());
        when(interviewRepository.findAll()).thenReturn(List.of(new Interview(), new Interview()));

        assertEquals(2, service.getAll().size());
    }
}