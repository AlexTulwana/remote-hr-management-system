package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.dto.EmployeeInviteEvent;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.model.Onboarding;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.OnboardingRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OnboardingFromApplicationServiceTest {

    @Mock private OnboardingRepository onboardingRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmployeeNumberGenerator employeeNumberGenerator;
    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private OnboardingService onboardingService;

    private User currentUser;
    private LocalDate startDate;

    @BeforeEach
    void setUp() {
        currentUser = new User();
        currentUser.setUsername("hrtest1");
        currentUser.setRole("HR");
        startDate = LocalDate.of(2026, 10, 1);
    }

    // ---- helpers ----

    private Branch branch(Long id, String name) {
        Branch b = new Branch();
        b.setId(id);
        b.setName(name);
        return b;
    }

    private Application acceptedApplication(Branch branch) {
        JobPosting posting = new JobPosting();
        posting.setTitle("Software Engineer");
        posting.setDepartment("Engineering");
        posting.setBranch(branch);

        Application a = new Application();
        a.setId(7L);
        a.setJobPosting(posting);
        a.setCandidateName("New Hire");
        a.setCandidateEmail("New.Hire@Example.com");
        a.setCandidatePhone("0821234567");
        a.setOutcome("ACCEPTED");
        return a;
    }

    private Employee managerEmployee(Branch branch) {
        Employee m = new Employee();
        m.setId(50L);
        m.setFullName("Sam Manager");
        m.setActive(true);
        m.setEmploymentStatus("ACTIVE");
        m.setBranch(branch);
        return m;
    }

    private User loginFor(Employee employee, String role) {
        User u = new User();
        u.setEmployee(employee);
        u.setRole(role);
        u.setUsername("login-" + role.toLowerCase());
        return u;
    }

    private void stubSaves() {
        when(passwordEncoder.encode(any())).thenReturn("encoded");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(onboardingRepository.save(any(Onboarding.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Onboarding start(String role, Long managerId) {
        return onboardingService.startFromApplication(7L, role, startDate, managerId, "welcome");
    }

    // ---- startFromApplication() ----

    @Test
    void startFromApplication_happyPath_createsEmployeeAccountOnboardingAndInvite() {
        Branch capeTown = branch(1L, "Cape Town");
        Employee manager = managerEmployee(capeTown);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(capeTown)));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(userRepository.findByEmployeeId(50L)).thenReturn(Optional.of(loginFor(manager, "MANAGER")));
        when(employeeNumberGenerator.next(1L)).thenReturn("EMP-2023");
        stubSaves();

        Onboarding result = start("EMPLOYEE", 50L);

        Employee created = result.getEmployee();
        assertEquals("EMP-2023", created.getEmployeeNumber());
        assertEquals("New Hire", created.getFullName());
        assertEquals("New.Hire@Example.com", created.getEmail());
        assertEquals("0821234567", created.getContactDetails());
        assertEquals("Software Engineer", created.getPosition());
        assertEquals("Engineering", created.getDepartment());
        assertEquals(capeTown, created.getBranch());
        assertEquals(manager, created.getReportsTo());
        assertEquals(startDate, created.getEmploymentDate());
        assertEquals("ONBOARDING", created.getEmploymentStatus());
        assertEquals("IN_PROGRESS", result.getStatus());
        assertEquals(7L, result.getApplication().getId());
        assertEquals(currentUser, result.getPerformedBy());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User account = userCaptor.getValue();
        assertEquals("new.hire@example.com", account.getUsername());
        assertEquals("EMPLOYEE", account.getRole());
        assertEquals(created, account.getEmployee());
        assertEquals("encoded", account.getPassword());
        assertNotNull(account.getResetToken());
        assertTrue(account.getResetTokenExpiry().isAfter(LocalDateTime.now().plusDays(6)));

        ArgumentCaptor<EmployeeInviteEvent> eventCaptor = ArgumentCaptor.forClass(EmployeeInviteEvent.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("employee.invited"), eventCaptor.capture());
        EmployeeInviteEvent event = eventCaptor.getValue();
        assertEquals("new.hire@example.com", event.email());
        assertEquals("New Hire", event.fullName());
        assertEquals("EMPLOYEE", event.role());
        assertEquals(account.getResetToken(), event.token());
    }

    @Test
    void startFromApplication_hrHire_hasNoBranchAndCanReportToHr() {
        Employee manager = managerEmployee(null);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(manager));
        when(userRepository.findByEmployeeId(50L)).thenReturn(Optional.of(loginFor(manager, "HR")));
        when(employeeNumberGenerator.next(null)).thenReturn("EMP-4002");
        stubSaves();

        Onboarding result = start("HR", 50L);

        assertNull(result.getEmployee().getBranch());
        assertEquals("EMP-4002", result.getEmployee().getEmployeeNumber());
        assertEquals(manager, result.getEmployee().getReportsTo());
    }

    @Test
    void startFromApplication_applicationNotFound_throws() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("Application not found", ex.getMessage());
    }

    @Test
    void startFromApplication_notAccepted_throws() {
        Application application = acceptedApplication(branch(1L, "Cape Town"));
        application.setOutcome("REJECTED");
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(application));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("Application has not been accepted", ex.getMessage());
    }

    @Test
    void startFromApplication_alreadyOnboarded_throws() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(onboardingRepository.existsByApplicationId(7L)).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("This candidate has already been onboarded", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_invalidRole_throws() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("SUPERUSER", 50L));

        assertEquals("Invalid role: SUPERUSER", ex.getMessage());
    }

    @Test
    void startFromApplication_adminRoleByNonAdmin_throwsAccessDenied() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        assertThrows(AccessDeniedException.class, () -> start("ADMIN", 50L));

        verify(userRepository, never()).save(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_duplicateUsername_throws() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(userRepository.findByUsername("new.hire@example.com")).thenReturn(Optional.of(new User()));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("A login for this email already exists", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_postingWithoutBranch_rejectsBranchRole() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(null)));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertTrue(ex.getMessage().contains("no branch"));
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_managerMissing_throws() {
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", null));

        assertEquals("A manager is required", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_managerIsPlainEmployee_throws() {
        Branch capeTown = branch(1L, "Cape Town");
        Employee notAManager = managerEmployee(capeTown);
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(capeTown)));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(notAManager));
        when(userRepository.findByEmployeeId(50L)).thenReturn(Optional.of(loginFor(notAManager, "EMPLOYEE")));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("Selected manager must be a Manager, HR or Admin", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    void startFromApplication_managerFromOtherBranch_throws() {
        Employee durbanManager = managerEmployee(branch(2L, "Durban"));
        when(applicationRepository.findById(7L)).thenReturn(Optional.of(acceptedApplication(branch(1L, "Cape Town"))));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);
        when(employeeRepository.findById(50L)).thenReturn(Optional.of(durbanManager));
        when(userRepository.findByEmployeeId(50L)).thenReturn(Optional.of(loginFor(durbanManager, "MANAGER")));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> start("EMPLOYEE", 50L));

        assertEquals("Manager must be in the same branch as the new hire", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    // ---- resendInvite() ----

    @Test
    void resendInvite_regeneratesTokenAndPublishes() {
        Employee employee = new Employee();
        employee.setId(60L);
        employee.setFullName("New Hire");
        Onboarding onboarding = new Onboarding();
        onboarding.setId(10L);
        onboarding.setEmployee(employee);
        onboarding.setStatus("IN_PROGRESS");

        User account = loginFor(employee, "EMPLOYEE");
        account.setUsername("new.hire@example.com");
        account.setResetToken("old-token");

        when(onboardingRepository.findById(10L)).thenReturn(Optional.of(onboarding));
        when(userRepository.findByEmployeeId(60L)).thenReturn(Optional.of(account));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(currentUserService.getCurrentUser()).thenReturn(currentUser);

        onboardingService.resendInvite(10L);

        assertNotEquals("old-token", account.getResetToken());
        assertTrue(account.getResetTokenExpiry().isAfter(LocalDateTime.now().plusDays(6)));
        verify(rabbitTemplate).convertAndSend(eq(RabbitMQConfig.EXCHANGE), eq("employee.invited"),
                any(EmployeeInviteEvent.class));
    }

    @Test
    void resendInvite_passwordAlreadySet_throwsAndSendsNothing() {
        Employee employee = new Employee();
        employee.setId(60L);
        Onboarding onboarding = new Onboarding();
        onboarding.setEmployee(employee);
        onboarding.setStatus("IN_PROGRESS");

        User account = loginFor(employee, "EMPLOYEE");
        account.setResetToken(null);

        when(onboardingRepository.findById(10L)).thenReturn(Optional.of(onboarding));
        when(userRepository.findByEmployeeId(60L)).thenReturn(Optional.of(account));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> onboardingService.resendInvite(10L));

        assertEquals("This person has already set their password", ex.getMessage());
        verifyNoInteractions(rabbitTemplate);
    }

    // ---- complete() guard ----

    @Test
    void complete_alreadyComplete_throwsAndChangesNothing() {
        Onboarding onboarding = new Onboarding();
        onboarding.setEmployee(new Employee());
        onboarding.setStatus("COMPLETE");
        when(onboardingRepository.findById(10L)).thenReturn(Optional.of(onboarding));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> onboardingService.complete(10L));

        assertEquals("Onboarding is already COMPLETE", ex.getMessage());
        verify(employeeRepository, never()).save(any());
    }

    // ---- getCandidates() / getManagerOptions() ----

    @Test
    void getCandidates_excludesAlreadyOnboarded() {
        Application onboarded = acceptedApplication(branch(1L, "Cape Town"));
        Application waiting = acceptedApplication(branch(1L, "Cape Town"));
        waiting.setId(8L);
        when(applicationRepository.findByOutcome("ACCEPTED")).thenReturn(List.of(onboarded, waiting));
        when(onboardingRepository.existsByApplicationId(7L)).thenReturn(true);
        when(onboardingRepository.existsByApplicationId(8L)).thenReturn(false);

        List<Application> result = onboardingService.getCandidates();

        assertEquals(1, result.size());
        assertEquals(8L, result.get(0).getId());
    }

    @Test
    void getManagerOptions_onlyActiveEmployeesWithAnEmployeeRecord() {
        Employee active = managerEmployee(branch(1L, "Cape Town"));
        Employee inactive = managerEmployee(branch(1L, "Cape Town"));
        inactive.setActive(false);
        User noEmployee = new User();
        noEmployee.setRole("HR");

        when(userRepository.findByRoleIn(any())).thenReturn(List.of(
                loginFor(active, "MANAGER"), loginFor(inactive, "MANAGER"), noEmployee));

        List<User> result = onboardingService.getManagerOptions();

        assertEquals(1, result.size());
        assertEquals(active, result.get(0).getEmployee());
    }
}
