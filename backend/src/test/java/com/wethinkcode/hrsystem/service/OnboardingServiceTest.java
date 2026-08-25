package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Onboarding;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.OnboardingRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingServiceTest {

    @Mock
    private OnboardingRepository onboardingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private OnboardingService onboardingService;

    private Employee employee;
    private User currentUser;
    private LocalDate startDate;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");

        currentUser = new User();
        currentUser.setUsername("hrtest2");

        startDate = LocalDate.of(2026, 9, 1);
    }

    // ---- start() ----

    @Test
    void start_noExistingOnboarding_createsAndSetsEmployeeStatus() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(onboardingRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(onboardingRepository.save(any(Onboarding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Onboarding result = onboardingService.start(1L, startDate, "Standard onboarding");

        assertEquals(employee, result.getEmployee());
        assertEquals(currentUser, result.getPerformedBy());
        assertEquals(startDate, result.getStartDate());
        assertEquals("Standard onboarding", result.getNotes());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals("ONBOARDING", employee.getEmploymentStatus());

        verify(employeeRepository).save(employee);
        verify(onboardingRepository).save(any(Onboarding.class));
    }

    @Test
    void start_employeeNotFound_throwsRuntimeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> onboardingService.start(1L, startDate, "notes"));
        assertEquals("Employee not found", ex.getMessage());

        verify(onboardingRepository, never()).save(any());
    }

    @Test
    void start_alreadyInProgress_throwsRuntimeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Onboarding existing = new Onboarding();
        existing.setStatus("IN_PROGRESS");
        when(onboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> onboardingService.start(1L, startDate, "notes"));
        assertEquals("Onboarding is already in progress for this employee", ex.getMessage());

        verify(onboardingRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void start_previousOnboardingComplete_allowsNewOne() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Onboarding previous = new Onboarding();
        previous.setStatus("COMPLETE");
        when(onboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(previous));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(onboardingRepository.save(any(Onboarding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Onboarding result = onboardingService.start(1L, startDate, "Re-onboarding");

        assertEquals("IN_PROGRESS", result.getStatus());
        verify(onboardingRepository).save(any(Onboarding.class));
    }

    // ---- complete() ----

    @Test
    void complete_setsStatusCompleteAndEmployeeActive() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(10L);
        onboarding.setEmployee(employee);
        onboarding.setStatus("IN_PROGRESS");
        when(onboardingRepository.findById(10L)).thenReturn(Optional.of(onboarding));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(onboardingRepository.save(any(Onboarding.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Onboarding result = onboardingService.complete(10L);

        assertEquals("COMPLETE", result.getStatus());
        assertEquals("ACTIVE", employee.getEmploymentStatus());
        verify(employeeRepository).save(employee);
    }

    @Test
    void complete_onboardingNotFound_throwsRuntimeException() {
        when(onboardingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> onboardingService.complete(99L));
        assertEquals("Onboarding record not found", ex.getMessage());

        verify(employeeRepository, never()).save(any());
    }

    // ---- getById() ----

    @Test
    void getById_found_returnsOnboarding() {
        Onboarding onboarding = new Onboarding();
        onboarding.setId(10L);
        when(onboardingRepository.findById(10L)).thenReturn(Optional.of(onboarding));

        Onboarding result = onboardingService.getById(10L);

        assertEquals(onboarding, result);
    }

    @Test
    void getById_notFound_throwsRuntimeException() {
        when(onboardingRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> onboardingService.getById(99L));
        assertEquals("Onboarding record not found", ex.getMessage());
    }

    // ---- getByEmployee() ----

    @Test
    void getByEmployee_returnsOnboardingsForThatEmployee() {
        List<Onboarding> records = List.of(new Onboarding(), new Onboarding());
        when(onboardingRepository.findByEmployeeId(1L)).thenReturn(records);

        List<Onboarding> result = onboardingService.getByEmployee(1L);

        assertEquals(2, result.size());
    }

    // ---- getAll() ----

    @Test
    void getAll_returnsAllOnboardings() {
        List<Onboarding> records = List.of(new Onboarding(), new Onboarding(), new Onboarding());
        when(onboardingRepository.findAll()).thenReturn(records);

        List<Onboarding> result = onboardingService.getAll();

        assertEquals(3, result.size());
    }
}