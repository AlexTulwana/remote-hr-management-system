package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Onboarding;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.OnboardingRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OnboardingService {

    private static final List<String> VALID_STATUSES = List.of("IN_PROGRESS", "COMPLETE");

    private final OnboardingRepository onboardingRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public OnboardingService(OnboardingRepository onboardingRepository,
                             EmployeeRepository employeeRepository,
                             CurrentUserService currentUserService) {
        this.onboardingRepository = onboardingRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
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

    public Onboarding complete(Long onboardingId) {
        Onboarding onboarding = getById(onboardingId);
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
}