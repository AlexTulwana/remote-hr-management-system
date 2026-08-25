package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EscalationRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Escalation;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.EscalationRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock
    private EscalationRepository escalationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private HearingRepository hearingRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EscalationService escalationService;

    private User reporter;
    private Employee aboutEmployee;
    private EscalationRequest request;

    @BeforeEach
    void setUp() {
        reporter = new User();
        reporter.setId(1L);
        reporter.setUsername("mgrtest1");

        aboutEmployee = new Employee();
        aboutEmployee.setId(2L);

        request = new EscalationRequest();
        request.setType("MANAGER_ESCALATION");
        request.setReason("Repeated tardiness affecting the team.");
    }

    // ---- submit() ----

    @Test
    void submit_derivesReporterFromCurrentUser_savesEscalation() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        when(escalationRepository.save(any(Escalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Escalation result = escalationService.submit(request);

        assertNotNull(result);
        assertEquals(reporter, result.getReporter());
        assertEquals("MANAGER_ESCALATION", result.getType());
        assertEquals("Repeated tardiness affecting the team.", result.getReason());
        assertEquals("SUBMITTED", result.getStatus());
        assertNotNull(result.getSubmittedAt());
        assertNull(result.getAboutEmployee());

        verify(escalationRepository).save(any(Escalation.class));
        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void submit_withAboutEmployeeId_setsAboutEmployee() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        request.setAboutEmployeeId(2L);
        when(employeeRepository.findById(2L)).thenReturn(Optional.of(aboutEmployee));
        when(escalationRepository.save(any(Escalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Escalation result = escalationService.submit(request);

        assertEquals(aboutEmployee, result.getAboutEmployee());
    }

    @Test
    void submit_aboutEmployeeNotFound_throwsRuntimeException() {
        when(currentUserService.getCurrentUser()).thenReturn(reporter);
        request.setAboutEmployeeId(99L);
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> escalationService.submit(request));
        assertEquals("Employee not found", ex.getMessage());

        verify(escalationRepository, never()).save(any());
    }

    // ---- linkToHearing() ----

    @Test
    void linkToHearing_setsLinkedHearingAndStatus() {
        Escalation escalation = new Escalation();
        escalation.setId(5L);
        when(escalationRepository.findById(5L)).thenReturn(Optional.of(escalation));

        Hearing hearing = new Hearing();
        hearing.setId(10L);
        when(hearingRepository.findById(10L)).thenReturn(Optional.of(hearing));

        when(escalationRepository.save(any(Escalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Escalation result = escalationService.linkToHearing(5L, 10L);

        assertEquals(hearing, result.getLinkedHearing());
        assertEquals("HEARING_SCHEDULED", result.getStatus());
    }

    @Test
    void linkToHearing_escalationNotFound_throwsRuntimeException() {
        when(escalationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> escalationService.linkToHearing(99L, 10L));
        assertEquals("Escalation not found", ex.getMessage());

        verify(hearingRepository, never()).findById(any());
    }

    @Test
    void linkToHearing_hearingNotFound_throwsRuntimeException() {
        Escalation escalation = new Escalation();
        escalation.setId(5L);
        when(escalationRepository.findById(5L)).thenReturn(Optional.of(escalation));
        when(hearingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> escalationService.linkToHearing(5L, 99L));
        assertEquals("Hearing not found", ex.getMessage());

        verify(escalationRepository, never()).save(any());
    }

    // ---- updateStatus() ----

    @Test
    void updateStatus_updatesAndSaves() {
        Escalation escalation = new Escalation();
        escalation.setId(5L);
        escalation.setStatus("SUBMITTED");
        when(escalationRepository.findById(5L)).thenReturn(Optional.of(escalation));
        when(escalationRepository.save(any(Escalation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Escalation result = escalationService.updateStatus(5L, "UNDER_REVIEW");

        assertEquals("UNDER_REVIEW", result.getStatus());
    }

    @Test
    void updateStatus_escalationNotFound_throwsRuntimeException() {
        when(escalationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> escalationService.updateStatus(99L, "RESOLVED"));
        assertEquals("Escalation not found", ex.getMessage());
    }

    // ---- getById() ----

    @Test
    void getById_found_returnsEscalation() {
        Escalation escalation = new Escalation();
        escalation.setId(5L);
        when(escalationRepository.findById(5L)).thenReturn(Optional.of(escalation));

        Escalation result = escalationService.getById(5L);

        assertEquals(escalation, result);
    }

    @Test
    void getById_notFound_throwsRuntimeException() {
        when(escalationRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> escalationService.getById(99L));
        assertEquals("Escalation not found", ex.getMessage());
    }

    // ---- getAll() ----

    @Test
    void getAll_returnsAllEscalations() {
        List<Escalation> escalations = List.of(new Escalation(), new Escalation());
        when(escalationRepository.findAll()).thenReturn(escalations);

        List<Escalation> result = escalationService.getAll();

        assertEquals(2, result.size());
    }
}