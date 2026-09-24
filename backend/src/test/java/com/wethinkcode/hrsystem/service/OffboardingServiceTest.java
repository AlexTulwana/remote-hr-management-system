package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Offboarding;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.OffboardingRepository;
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
class OffboardingServiceTest {

    @Mock
    private OffboardingRepository offboardingRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private OffboardingService offboardingService;

    private Employee employee;
    private User currentUser;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");
        employee.setActive(true);
        employee.setEmploymentStatus("ACTIVE");

        currentUser = new User();
        currentUser.setId(99L);
        currentUser.setUsername("hrtest2");
        currentUser.setRole("HR");
    }

    // ---------- start() ----------

    @Test
    void start_invalidType_throwsException() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.start(1L, "RETIREMENT", LocalDate.now(), "reason"));

        assertTrue(ex.getMessage().contains("Invalid offboarding type"));
        verifyNoInteractions(employeeRepository, offboardingRepository);
    }

    @Test
    void start_employeeNotFound_throwsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.start(1L, "RESIGNATION", LocalDate.now(), "reason"));

        assertEquals("Employee not found", ex.getMessage());
    }

    @Test
    void start_alreadyInProgress_throwsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Offboarding existing = new Offboarding();
        existing.setStatus("IN_PROGRESS");
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.start(1L, "RESIGNATION", LocalDate.now(), "reason"));

        assertTrue(ex.getMessage().contains("already in progress"));
        verify(offboardingRepository, never()).save(any());
    }

    @Test
    void start_previousCompletedOffboarding_doesNotBlockNewOne() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Offboarding oldOne = new Offboarding();
        oldOne.setStatus("COMPLETE");
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(oldOne));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate effective = LocalDate.of(2026, 9, 1);
        Offboarding result = offboardingService.start(1L, "RESIGNATION", effective, "moving on");

        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(employee, result.getEmployee());
    }

    @Test
    void start_resignation_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        LocalDate effective = LocalDate.of(2026, 9, 15);
        Offboarding result = offboardingService.start(1L, "RESIGNATION", effective, "better opportunity");

        assertEquals(employee, result.getEmployee());
        assertEquals(currentUser, result.getPerformedBy());
        assertEquals("RESIGNATION", result.getType());
        assertEquals(effective, result.getEffectiveDate());
        assertEquals("better opportunity", result.getReason());
        assertEquals("IN_PROGRESS", result.getStatus());
        verify(offboardingRepository).save(result);
    }

    @Test
    void start_termination_success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        Offboarding result = offboardingService.start(1L, "TERMINATION", LocalDate.now(), "misconduct");

        assertEquals("TERMINATION", result.getType());
        assertEquals("IN_PROGRESS", result.getStatus());
    }

    // ---------- complete() ----------

    @Test
    void complete_notFound_throwsException() {
        when(offboardingRepository.findById(5L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.complete(5L));

        assertEquals("Offboarding record not found", ex.getMessage());
    }

    @Test
    void complete_resignation_updatesEmployeeCorrectly() {
        LocalDate effective = LocalDate.now().minusDays(1);
        Offboarding offboarding = new Offboarding();
        offboarding.setId(5L);
        offboarding.setEmployee(employee);
        offboarding.setType("RESIGNATION");
        offboarding.setEffectiveDate(effective);
        offboarding.setStatus("IN_PROGRESS");

        when(offboardingRepository.findById(5L)).thenReturn(Optional.of(offboarding));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        Offboarding result = offboardingService.complete(5L);

        assertEquals("COMPLETE", result.getStatus());
        assertFalse(employee.isActive());
        assertEquals("RESIGNED", employee.getEmploymentStatus());
        assertEquals(effective, employee.getResignationDate());
        assertNull(employee.getTerminationDate());
        verify(employeeRepository).save(employee);
        verify(offboardingRepository).save(offboarding);
    }

    @Test
    void complete_termination_updatesEmployeeCorrectly() {
        LocalDate effective = LocalDate.now().minusDays(1);
        Offboarding offboarding = new Offboarding();
        offboarding.setId(6L);
        offboarding.setEmployee(employee);
        offboarding.setType("TERMINATION");
        offboarding.setEffectiveDate(effective);
        offboarding.setStatus("IN_PROGRESS");

        when(offboardingRepository.findById(6L)).thenReturn(Optional.of(offboarding));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        Offboarding result = offboardingService.complete(6L);

        assertEquals("COMPLETE", result.getStatus());
        assertFalse(employee.isActive());
        assertEquals("TERMINATED", employee.getEmploymentStatus());
        assertEquals(effective, employee.getTerminationDate());
        assertNull(employee.getResignationDate());
    }

    @Test
    void start_scheduledOffboardingExists_throwsException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        Offboarding existing = new Offboarding();
        existing.setStatus("SCHEDULED");
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(existing));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.start(1L, "RESIGNATION", LocalDate.now(), "reason"));

        assertTrue(ex.getMessage().contains("already in progress"));
        verify(offboardingRepository, never()).save(any());
    }

    @Test
    void complete_futureDate_schedulesAndLeavesEmployeeUntouched() {
        Offboarding offboarding = new Offboarding();
        offboarding.setId(10L);
        offboarding.setEmployee(employee);
        offboarding.setType("TERMINATION");
        offboarding.setEffectiveDate(LocalDate.now().plusDays(5));
        offboarding.setStatus("IN_PROGRESS");

        when(offboardingRepository.findById(10L)).thenReturn(Optional.of(offboarding));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        Offboarding result = offboardingService.complete(10L);

        assertEquals("SCHEDULED", result.getStatus());
        assertTrue(employee.isActive());
        assertEquals("ACTIVE", employee.getEmploymentStatus());
        assertNull(employee.getTerminationDate());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void complete_effectiveToday_appliesImmediately() {
        Offboarding offboarding = new Offboarding();
        offboarding.setId(11L);
        offboarding.setEmployee(employee);
        offboarding.setType("RESIGNATION");
        offboarding.setEffectiveDate(LocalDate.now());
        offboarding.setStatus("IN_PROGRESS");

        when(offboardingRepository.findById(11L)).thenReturn(Optional.of(offboarding));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        Offboarding result = offboardingService.complete(11L);

        assertEquals("COMPLETE", result.getStatus());
        assertFalse(employee.isActive());
        assertEquals("RESIGNED", employee.getEmploymentStatus());
    }

    @Test
    void complete_alreadyComplete_throwsAndChangesNothing() {
        Offboarding offboarding = new Offboarding();
        offboarding.setId(12L);
        offboarding.setEmployee(employee);
        offboarding.setStatus("COMPLETE");
        when(offboardingRepository.findById(12L)).thenReturn(Optional.of(offboarding));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.complete(12L));

        assertEquals("Offboarding is already COMPLETE", ex.getMessage());
        verify(employeeRepository, never()).save(any());
        verify(offboardingRepository, never()).save(any());
    }

    @Test
    void complete_alreadyScheduled_throwsAndChangesNothing() {
        Offboarding offboarding = new Offboarding();
        offboarding.setId(13L);
        offboarding.setEmployee(employee);
        offboarding.setStatus("SCHEDULED");
        when(offboardingRepository.findById(13L)).thenReturn(Optional.of(offboarding));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> offboardingService.complete(13L));

        assertEquals("Offboarding is already SCHEDULED", ex.getMessage());
        verify(employeeRepository, never()).save(any());
        verify(offboardingRepository, never()).save(any());
    }

    // ---------- applyDueOffboardings() ----------

    @Test
    void applyDueOffboardings_deactivatesEachDueEmployee() {
        Offboarding due = new Offboarding();
        due.setId(20L);
        due.setEmployee(employee);
        due.setType("RESIGNATION");
        due.setEffectiveDate(LocalDate.now().minusDays(1));
        due.setStatus("SCHEDULED");

        when(offboardingRepository.findByStatusAndEffectiveDateLessThanEqual(eq("SCHEDULED"), any(LocalDate.class)))
                .thenReturn(List.of(due));
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(offboardingRepository.save(any(Offboarding.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = offboardingService.applyDueOffboardings();

        assertEquals(1, count);
        assertEquals("COMPLETE", due.getStatus());
        assertFalse(employee.isActive());
        assertEquals("RESIGNED", employee.getEmploymentStatus());
        assertEquals(due.getEffectiveDate(), employee.getResignationDate());
    }

    @Test
    void applyDueOffboardings_nothingDue_changesNothing() {
        when(offboardingRepository.findByStatusAndEffectiveDateLessThanEqual(eq("SCHEDULED"), any(LocalDate.class)))
                .thenReturn(List.of());

        int count = offboardingService.applyDueOffboardings();

        assertEquals(0, count);
        verify(employeeRepository, never()).save(any());
    }

    // ---------- getById() ----------

    @Test
    void getById_found_returnsOffboarding() {
        Offboarding offboarding = new Offboarding();
        offboarding.setId(7L);
        when(offboardingRepository.findById(7L)).thenReturn(Optional.of(offboarding));

        Offboarding result = offboardingService.getById(7L);

        assertEquals(offboarding, result);
    }

    @Test
    void getById_notFound_throwsException() {
        when(offboardingRepository.findById(8L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> offboardingService.getById(8L));
    }

    // ---------- getByEmployee() / getAll() ----------

    @Test
    void getByEmployee_delegatesToRepository() {
        Offboarding o = new Offboarding();
        when(offboardingRepository.findByEmployeeId(1L)).thenReturn(List.of(o));

        List<Offboarding> result = offboardingService.getByEmployee(1L);

        assertEquals(1, result.size());
        assertSame(o, result.get(0));
    }

    @Test
    void getAll_delegatesToRepository() {
        when(offboardingRepository.findAll()).thenReturn(List.of(new Offboarding(), new Offboarding()));

        List<Offboarding> result = offboardingService.getAll();

        assertEquals(2, result.size());
    }
}