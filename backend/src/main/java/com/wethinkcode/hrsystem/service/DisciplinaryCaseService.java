package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.DisciplinaryCaseRequest;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DisciplinaryCaseService {

    private final DisciplinaryCaseRepository caseRepository;
    private final DisciplinaryCaseHistoryRepository historyRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final EscalationRepository escalationRepository;
    private final HearingRepository hearingRepository;

    public DisciplinaryCaseService(DisciplinaryCaseRepository caseRepository,
                                    DisciplinaryCaseHistoryRepository historyRepository,
                                    EmployeeRepository employeeRepository,
                                    UserRepository userRepository,
                                    EscalationRepository escalationRepository,
                                    HearingRepository hearingRepository) {
        this.caseRepository = caseRepository;
        this.historyRepository = historyRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.escalationRepository = escalationRepository;
        this.hearingRepository = hearingRepository;
    }

    public DisciplinaryCase open(DisciplinaryCaseRequest request, String username) {
        User opener = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        DisciplinaryStage initialStage;
        try {
            initialStage = DisciplinaryStage.valueOf(request.getInitialStage());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid stage: " + request.getInitialStage());
        }

        // Managers may only open verbal/written warnings, and only for their own branch
        if ("MANAGER".equals(opener.getRole())) {
            if (initialStage != DisciplinaryStage.VERBAL_WARNING && initialStage != DisciplinaryStage.WRITTEN_WARNING) {
                throw new AccessDeniedException("Managers can only issue verbal or written warnings");
            }
            Long managerBranchId = opener.getEmployee() != null && opener.getEmployee().getBranch() != null
                    ? opener.getEmployee().getBranch().getId() : null;
            Long employeeBranchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
            if (managerBranchId == null || employeeBranchId == null || !managerBranchId.equals(employeeBranchId)) {
                throw new AccessDeniedException("Managers can only open cases for their own branch");
            }
        }

        DisciplinaryCase disciplinaryCase = new DisciplinaryCase();
        disciplinaryCase.setEmployee(employee);
        disciplinaryCase.setReason(request.getReason());
        disciplinaryCase.setCurrentStage(initialStage);
        disciplinaryCase.setOpenedBy(opener);
        disciplinaryCase.setOpenedAt(LocalDateTime.now());

        if (request.getLinkedEscalationId() != null) {
            Escalation escalation = escalationRepository.findById(request.getLinkedEscalationId())
                    .orElseThrow(() -> new RuntimeException("Escalation not found"));
            disciplinaryCase.setLinkedEscalation(escalation);
        }

        DisciplinaryCase saved = caseRepository.save(disciplinaryCase);
        logStage(saved, initialStage, opener, request.getReason(), null);
        return saved;
    }

    public DisciplinaryCase progressStage(Long caseId, String newStageStr, String comment,
                                           Long linkedHearingId, String username) {
        User actor = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        DisciplinaryCase disciplinaryCase = getById(caseId);
        if (disciplinaryCase.isClosed()) {
            throw new RuntimeException("Cannot progress a closed case");
        }

        DisciplinaryStage newStage;
        try {
            newStage = DisciplinaryStage.valueOf(newStageStr);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid stage: " + newStageStr);
        }

        // Managers may only ever move a case between verbal/written warning stages, own branch only
        if ("MANAGER".equals(actor.getRole())) {
            if (newStage != DisciplinaryStage.VERBAL_WARNING && newStage != DisciplinaryStage.WRITTEN_WARNING) {
                throw new AccessDeniedException("Managers can only issue verbal or written warnings");
            }
            Long managerBranchId = actor.getEmployee() != null && actor.getEmployee().getBranch() != null
                    ? actor.getEmployee().getBranch().getId() : null;
            Long employeeBranchId = disciplinaryCase.getEmployee().getBranch() != null
                    ? disciplinaryCase.getEmployee().getBranch().getId() : null;
            if (managerBranchId == null || employeeBranchId == null || !managerBranchId.equals(employeeBranchId)) {
                throw new AccessDeniedException("Managers can only progress cases for their own branch");
            }
        }

        Hearing linkedHearing = null;
        if (linkedHearingId != null) {
            linkedHearing = hearingRepository.findById(linkedHearingId)
                    .orElseThrow(() -> new RuntimeException("Hearing not found"));
        }

        disciplinaryCase.setCurrentStage(newStage);
        if (newStage == DisciplinaryStage.RESOLVED || newStage == DisciplinaryStage.CLOSED
                || newStage == DisciplinaryStage.DISMISSAL) {
            disciplinaryCase.setClosed(true);
        }

        DisciplinaryCase saved = caseRepository.save(disciplinaryCase);
        logStage(saved, newStage, actor, comment, linkedHearing);
        return saved;
    }

    private void logStage(DisciplinaryCase disciplinaryCase, DisciplinaryStage stage, User actor,
                           String comment, Hearing linkedHearing) {
        DisciplinaryCaseHistory entry = new DisciplinaryCaseHistory();
        entry.setDisciplinaryCase(disciplinaryCase);
        entry.setStage(stage);
        entry.setActionedBy(actor);
        entry.setComment(comment);
        entry.setActionedAt(LocalDateTime.now());
        entry.setLinkedHearing(linkedHearing);
        historyRepository.save(entry);
    }

    public DisciplinaryCase getById(Long id) {
        return caseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Disciplinary case not found"));
    }

    public List<DisciplinaryCaseHistory> getHistory(Long caseId) {
        return historyRepository.findByDisciplinaryCaseIdOrderByActionedAtAsc(caseId);
    }

    public List<DisciplinaryCase> getByEmployee(Long employeeId) {
        return caseRepository.findByEmployeeId(employeeId);
    }

    public List<DisciplinaryCase> getByEmployeeForViewer(Long employeeId, String username) {
        User viewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isHrOrAdmin = "HR".equals(viewer.getRole()) || "ADMIN".equals(viewer.getRole());
        if (isHrOrAdmin) {
            return caseRepository.findByEmployeeId(employeeId);
        }

        Employee target = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if ("MANAGER".equals(viewer.getRole())) {
            Long managerBranchId = viewer.getEmployee() != null && viewer.getEmployee().getBranch() != null
                    ? viewer.getEmployee().getBranch().getId() : null;
            Long targetBranchId = target.getBranch() != null ? target.getBranch().getId() : null;
            if (managerBranchId == null || targetBranchId == null || !managerBranchId.equals(targetBranchId)) {
                throw new AccessDeniedException("Managers can only view cases for their own branch");
            }
            return caseRepository.findByEmployeeId(employeeId);
        }

        // Employee: only their own
        Long viewerEmployeeId = viewer.getEmployee() != null ? viewer.getEmployee().getId() : null;
        if (viewerEmployeeId == null || !viewerEmployeeId.equals(employeeId)) {
            throw new AccessDeniedException("You can only view your own disciplinary case history");
        }
        return caseRepository.findByEmployeeId(employeeId);
    }

    public List<DisciplinaryCase> getByBranch(Long branchId) {
        return caseRepository.findByEmployeeBranchId(branchId);
    }

    public List<DisciplinaryCase> getAllOpen() {
        return caseRepository.findByClosedFalse();
    }

    public List<DisciplinaryCase> getAll() {
        return caseRepository.findAll();
    }
}
