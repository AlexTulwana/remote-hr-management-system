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
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EscalationService {

    private final EscalationRepository escalationRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final HearingRepository hearingRepository;

    public EscalationService(EscalationRepository escalationRepository,
                             UserRepository userRepository,
                             EmployeeRepository employeeRepository,
                             HearingRepository hearingRepository) {
        this.escalationRepository = escalationRepository;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.hearingRepository = hearingRepository;
    }

    public Escalation submit(EscalationRequest request) {
        User reporter = userRepository.findById(request.getReporterId())
                .orElseThrow(() -> new RuntimeException("Reporter not found"));

        Escalation escalation = new Escalation();
        escalation.setReporter(reporter);
        escalation.setType(request.getType());
        escalation.setReason(request.getReason());
        escalation.setSubmittedAt(LocalDateTime.now());
        escalation.setStatus("SUBMITTED");

        if (request.getAboutEmployeeId() != null) {
            Employee aboutEmployee = employeeRepository.findById(request.getAboutEmployeeId())
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            escalation.setAboutEmployee(aboutEmployee);
        }

        // Placeholder - Phase 24 (Notifications) will replace this with a real HR alert.
        System.out.println("NEW ESCALATION/COMPLAINT (" + escalation.getType() + ") submitted by "
                + reporter.getUsername() + ": " + request.getReason());

        return escalationRepository.save(escalation);
    }

    public Escalation linkToHearing(Long escalationId, Long hearingId) {
        Escalation escalation = getById(escalationId);
        Hearing hearing = hearingRepository.findById(hearingId)
                .orElseThrow(() -> new RuntimeException("Hearing not found"));
        escalation.setLinkedHearing(hearing);
        escalation.setStatus("HEARING_SCHEDULED");
        return escalationRepository.save(escalation);
    }

    public Escalation updateStatus(Long id, String status) {
        Escalation escalation = getById(id);
        escalation.setStatus(status);
        return escalationRepository.save(escalation);
    }

    public Escalation getById(Long id) {
        return escalationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Escalation not found"));
    }

    // NOTE: this returns everything - only HR/Admin should be allowed to call this endpoint.
    // Role enforcement happens at the controller/security layer (refined further in Phase 16).
    public List<Escalation> getAll() {
        return escalationRepository.findAll();
    }
}