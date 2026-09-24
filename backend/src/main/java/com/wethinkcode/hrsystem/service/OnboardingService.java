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
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class OnboardingService {

    private static final List<String> VALID_STATUSES = List.of("IN_PROGRESS", "COMPLETE");
    private static final Set<String> VALID_ROLES = Set.of("EMPLOYEE", "MANAGER", "HR", "ADMIN");
    private static final Set<String> MANAGER_ROLES = Set.of("MANAGER", "HR", "ADMIN");
    private static final Set<String> NO_BRANCH_ROLES = Set.of("HR", "ADMIN");
    private static final int INVITE_VALID_DAYS = 7;
    private static final String INVITE_ROUTING_KEY = "employee.invited";

    private final OnboardingRepository onboardingRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmployeeNumberGenerator employeeNumberGenerator;
    private final RabbitTemplate rabbitTemplate;

    public OnboardingService(OnboardingRepository onboardingRepository,
                             EmployeeRepository employeeRepository,
                             CurrentUserService currentUserService,
                             ApplicationRepository applicationRepository,
                             UserRepository userRepository,
                             PasswordEncoder passwordEncoder,
                             EmployeeNumberGenerator employeeNumberGenerator,
                             RabbitTemplate rabbitTemplate) {
        this.onboardingRepository = onboardingRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.employeeNumberGenerator = employeeNumberGenerator;
        this.rabbitTemplate = rabbitTemplate;
    }

    public Onboarding start(Long employeeId, LocalDate startDate, String notes) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        boolean alreadyInProgress = onboardingRepository.findByEmployeeId(employeeId)
                .stream().anyMatch(o -> "IN_PROGRESS".equals(o.getStatus()));
        if (alreadyInProgress) {
            throw new RuntimeException("Onboarding is already in progress for this employee");
        }

        User currentUser = currentUserService.getCurrentUser();

        Onboarding onboarding = new Onboarding();
        onboarding.setEmployee(employee);
        onboarding.setPerformedBy(currentUser);
        onboarding.setStartDate(startDate);
        onboarding.setNotes(notes);
        onboarding.setStatus("IN_PROGRESS");

        employee.setEmploymentStatus("ONBOARDING");
        employeeRepository.save(employee);

        Onboarding saved = onboardingRepository.save(onboarding);

        System.out.println("AUDIT: " + currentUser.getUsername() + " started onboarding for "
                + employee.getFullName() + " on " + startDate);

        return saved;
    }

    // Accepted candidates that have not been onboarded yet
    public List<Application> getCandidates() {
        return applicationRepository.findByOutcome("ACCEPTED").stream()
                .filter(a -> !onboardingRepository.existsByApplicationId(a.getId()))
                .toList();
    }

    // Active people who can be a new hire's manager (login role MANAGER, HR or ADMIN)
    public List<User> getManagerOptions() {
        return userRepository.findByRoleIn(MANAGER_ROLES).stream()
                .filter(u -> u.getEmployee() != null
                        && u.getEmployee().isActive()
                        && "ACTIVE".equals(u.getEmployee().getEmploymentStatus()))
                .toList();
    }

    @Transactional
    public Onboarding startFromApplication(Long applicationId, String role, LocalDate startDate,
                                           Long reportsToId, String notes) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new RuntimeException("Application not found"));
        if (!"ACCEPTED".equals(application.getOutcome())) {
            throw new RuntimeException("Application has not been accepted");
        }
        if (onboardingRepository.existsByApplicationId(applicationId)) {
            throw new RuntimeException("This candidate has already been onboarded");
        }
        if (role == null || !VALID_ROLES.contains(role)) {
            throw new RuntimeException("Invalid role: " + role);
        }

        User currentUser = currentUserService.getCurrentUser();
        if ("ADMIN".equals(role) && !"ADMIN".equals(currentUser.getRole())) {
            throw new AccessDeniedException("Only Admin can onboard Admin accounts");
        }
        if (startDate == null) {
            throw new RuntimeException("Start date is required");
        }

        String email = application.getCandidateEmail();
        if (email == null || email.isBlank()) {
            throw new RuntimeException("Candidate has no email address");
        }
        String username = email.trim().toLowerCase();
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("A login for this email already exists");
        }

        JobPosting posting = application.getJobPosting();
        boolean noBranchRole = NO_BRANCH_ROLES.contains(role);
        Branch branch = noBranchRole ? null : posting.getBranch();
        if (!noBranchRole && branch == null) {
            throw new RuntimeException("The job posting has no branch, so this hire cannot be placed in a branch");
        }

        Employee manager = requireValidManager(reportsToId, branch);

        Employee employee = new Employee();
        employee.setEmployeeNumber(employeeNumberGenerator.next(branch != null ? branch.getId() : null));
        employee.setFullName(application.getCandidateName());
        employee.setEmail(email.trim());
        employee.setContactDetails(application.getCandidatePhone());
        employee.setPosition(posting.getTitle());
        employee.setDepartment(posting.getDepartment());
        employee.setBranch(branch);
        employee.setEmploymentDate(startDate);
        employee.setReportsTo(manager);
        employee.setEmploymentStatus("ONBOARDING");
        Employee savedEmployee = employeeRepository.save(employee);

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusDays(INVITE_VALID_DAYS);

        User account = new User();
        account.setUsername(username);
        account.setRole(role);
        account.setEmployee(savedEmployee);
        account.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        account.setResetToken(token);
        account.setResetTokenExpiry(expiry);
        userRepository.save(account);

        Onboarding onboarding = new Onboarding();
        onboarding.setEmployee(savedEmployee);
        onboarding.setPerformedBy(currentUser);
        onboarding.setApplication(application);
        onboarding.setStartDate(startDate);
        onboarding.setNotes(notes);
        onboarding.setStatus("IN_PROGRESS");
        Onboarding saved = onboardingRepository.save(onboarding);

        publishInvite(new EmployeeInviteEvent(username, savedEmployee.getFullName(), role, token, expiry));

        System.out.println("AUDIT: " + currentUser.getUsername() + " onboarded "
                + savedEmployee.getFullName() + " (" + savedEmployee.getEmployeeNumber() + ") as " + role);

        return saved;
    }

    @Transactional
    public void resendInvite(Long onboardingId) {
        Onboarding onboarding = getById(onboardingId);
        if (!"IN_PROGRESS".equals(onboarding.getStatus())) {
            throw new RuntimeException("Onboarding is not in progress");
        }

        User account = userRepository.findByEmployeeId(onboarding.getEmployee().getId())
                .orElseThrow(() -> new RuntimeException("No login account exists for this onboarding"));
        if (account.getResetToken() == null) {
            throw new RuntimeException("This person has already set their password");
        }

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusDays(INVITE_VALID_DAYS);
        account.setResetToken(token);
        account.setResetTokenExpiry(expiry);
        userRepository.save(account);

        publishInvite(new EmployeeInviteEvent(account.getUsername(), onboarding.getEmployee().getFullName(),
                account.getRole(), token, expiry));

        System.out.println("AUDIT: " + currentUserService.getCurrentUser().getUsername()
                + " resent onboarding invite to " + account.getUsername());
    }

    public Onboarding complete(Long onboardingId) {
        Onboarding onboarding = getById(onboardingId);
        if (!"IN_PROGRESS".equals(onboarding.getStatus())) {
            throw new RuntimeException("Onboarding is already " + onboarding.getStatus());
        }
        onboarding.setStatus("COMPLETE");

        Employee employee = onboarding.getEmployee();
        employee.setEmploymentStatus("ACTIVE");
        employeeRepository.save(employee);

        Onboarding saved = onboardingRepository.save(onboarding);

        System.out.println("AUDIT: " + currentUserService.getCurrentUser().getUsername()
                + " completed onboarding for " + employee.getFullName());

        return saved;
    }

    public Onboarding getById(Long id) {
        return onboardingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Onboarding record not found"));
    }

    public List<Onboarding> getByEmployee(Long employeeId) {
        return onboardingRepository.findByEmployeeId(employeeId);
    }

    public List<Onboarding> getAll() {
        return onboardingRepository.findAll();
    }

    private Employee requireValidManager(Long managerId, Branch hireBranch) {
        if (managerId == null) {
            throw new RuntimeException("A manager is required");
        }
        Employee manager = employeeRepository.findById(managerId)
                .orElseThrow(() -> new RuntimeException("Manager not found"));
        if (!manager.isActive() || !"ACTIVE".equals(manager.getEmploymentStatus())) {
            throw new RuntimeException("Manager is not an active employee");
        }
        User managerUser = userRepository.findByEmployeeId(manager.getId())
                .orElseThrow(() -> new RuntimeException("Selected manager has no login account"));
        if (!MANAGER_ROLES.contains(managerUser.getRole())) {
            throw new RuntimeException("Selected manager must be a Manager, HR or Admin");
        }
        boolean branchScopedManager = "MANAGER".equals(managerUser.getRole());
        if (hireBranch != null && branchScopedManager
                && (manager.getBranch() == null || !hireBranch.getId().equals(manager.getBranch().getId()))) {
            throw new RuntimeException("Manager must be in the same branch as the new hire");
        }
        return manager;
    }

    // Publishes after the database commit so the email never goes out for a rolled-back hire
    private void publishInvite(EmployeeInviteEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendInvite(event);
                }
            });
        } else {
            sendInvite(event);
        }
    }

    private void sendInvite(EmployeeInviteEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, INVITE_ROUTING_KEY, event);
    }
}
