package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.Offboarding;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.OffboardingRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class OffboardingService {

    private static final List<String> VALID_TYPES = List.of("RESIGNATION", "TERMINATION");
    private static final List<String> VALID_STATUSES = List.of("IN_PROGRESS", "SCHEDULED", "COMPLETE");

    private final OffboardingRepository offboardingRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public OffboardingService(OffboardingRepository offboardingRepository,
                              EmployeeRepository employeeRepository,
                              CurrentUserService currentUserService) {
        this.offboardingRepository = offboardingRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    public Offboarding start(Long employeeId, String type, LocalDate effectiveDate, String reason) {
        if (!VALID_TYPES.contains(type)) {
            throw new RuntimeException("Invalid offboarding type: " + type);
        }

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        boolean alreadyInProgress = offboardingRepository.findByEmployeeId(employeeId)
                .stream().anyMatch(o -> "IN_PROGRESS".equals(o.getStatus())
                        || "SCHEDULED".equals(o.getStatus()));
        if (alreadyInProgress) {
            throw new RuntimeException("Offboarding is already in progress for this employee");
        }

        User currentUser = currentUserService.getCurrentUser();

        Offboarding offboarding = new Offboarding();
        offboarding.setEmployee(employee);
        offboarding.setPerformedBy(currentUser);
        offboarding.setType(type);
        offboarding.setEffectiveDate(effectiveDate);
        offboarding.setReason(reason);
        offboarding.setStatus("IN_PROGRESS");

        Offboarding saved = offboardingRepository.save(offboarding);

        System.out.println("AUDIT: " + currentUser.getUsername() + " started " + type
                + " offboarding for " + employee.getFullName() + ", effective " + effectiveDate);

        return saved;
    }

    public Offboarding complete(Long offboardingId) {
        Offboarding offboarding = getById(offboardingId);
        if (!"IN_PROGRESS".equals(offboarding.getStatus())) {
            throw new RuntimeException("Offboarding is already " + offboarding.getStatus());
        }

        String username = currentUserService.getCurrentUser().getUsername();
        LocalDate effectiveDate = offboarding.getEffectiveDate();

        if (effectiveDate == null || !effectiveDate.isAfter(LocalDate.now())) {
            Offboarding saved = applyOffboarding(offboarding);
            System.out.println("AUDIT: " + username + " completed offboarding for "
                    + offboarding.getEmployee().getFullName());
            return saved;
        }

        offboarding.setStatus("SCHEDULED");
        Offboarding saved = offboardingRepository.save(offboarding);
        System.out.println("AUDIT: " + username + " scheduled offboarding for "
                + offboarding.getEmployee().getFullName() + ", takes effect " + effectiveDate);
        return saved;
    }

    @Scheduled(cron = "0 1 0 * * *")
    public int applyDueOffboardings() {
        List<Offboarding> due = offboardingRepository
                .findByStatusAndEffectiveDateLessThanEqual("SCHEDULED", LocalDate.now());

        for (Offboarding offboarding : due) {
            applyOffboarding(offboarding);
            System.out.println("AUDIT: scheduled offboarding applied for "
                    + offboarding.getEmployee().getFullName() + ", effective " + offboarding.getEffectiveDate());
        }
        return due.size();
    }

    private Offboarding applyOffboarding(Offboarding offboarding) {
        Employee employee = offboarding.getEmployee();
        employee.setActive(false);
        if ("RESIGNATION".equals(offboarding.getType())) {
            employee.setEmploymentStatus("RESIGNED");
            employee.setResignationDate(offboarding.getEffectiveDate());
        } else {
            employee.setEmploymentStatus("TERMINATED");
            employee.setTerminationDate(offboarding.getEffectiveDate());
        }
        employeeRepository.save(employee);

        offboarding.setStatus("COMPLETE");
        return offboardingRepository.save(offboarding);
    }

    public Offboarding getById(Long id) {
        return offboardingRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offboarding record not found"));
    }

    public List<Offboarding> getByEmployee(Long employeeId) {
        return offboardingRepository.findByEmployeeId(employeeId);
    }

    public List<Offboarding> getAll() {
        return offboardingRepository.findAll();
    }
}