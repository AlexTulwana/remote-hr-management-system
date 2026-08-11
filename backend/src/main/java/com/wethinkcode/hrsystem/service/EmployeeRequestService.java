package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeRequestSubmission;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.RequestType;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class EmployeeRequestService {

    private final EmployeeRequestRepository employeeRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public EmployeeRequestService(EmployeeRequestRepository employeeRequestRepository,
                                   EmployeeRepository employeeRepository,
                                   UserRepository userRepository) {
        this.employeeRequestRepository = employeeRequestRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
    }

    public EmployeeRequest submit(EmployeeRequestSubmission submission, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Employee employee = user.getEmployee();
        if (employee == null) {
            throw new RuntimeException("This account is not linked to an employee record");
        }

        RequestType type;
        try {
            type = RequestType.valueOf(submission.getRequestType());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid request type: " + submission.getRequestType());
        }

        EmployeeRequest request = new EmployeeRequest();
        request.setEmployee(employee);
        request.setRequestType(type);
        request.setDescription(submission.getDescription());
        request.setSubmittedAt(LocalDateTime.now());
        request.setStatus("PENDING");

        return employeeRequestRepository.save(request);
    }

    public EmployeeRequest managerDecision(Long requestId, String decision, String comment, String username) {
        EmployeeRequest request = getById(requestId);
        User manager = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("Only pending requests can be actioned by a manager");
        }

        // Managers can only action requests for employees in their own branch
        Long managerBranchId = manager.getEmployee() != null && manager.getEmployee().getBranch() != null
                ? manager.getEmployee().getBranch().getId() : null;
        Long requestBranchId = request.getEmployee().getBranch() != null
                ? request.getEmployee().getBranch().getId() : null;

        if (managerBranchId == null || requestBranchId == null || !managerBranchId.equals(requestBranchId)) {
            throw new AccessDeniedException("Managers can only action requests for their own branch");
        }

        switch (decision) {
            case "APPROVED", "REJECTED" -> {
                request.setStatus(decision);
                request.setResolvedAt(LocalDateTime.now());
            }
            case "ESCALATED" -> request.setStatus("ESCALATED");
            default -> throw new RuntimeException("Invalid decision: " + decision);
        }

        request.setManagerComment(comment);
        request.setHandledBy(manager);
        return employeeRequestRepository.save(request);
    }

    public EmployeeRequest hrDecision(Long requestId, String decision, String comment, String username) {
        EmployeeRequest request = getById(requestId);
        User hrUser = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!"ESCALATED".equals(request.getStatus())) {
            throw new RuntimeException("Only escalated requests can be given a final HR decision");
        }

        if (!"APPROVED".equals(decision) && !"REJECTED".equals(decision)) {
            throw new RuntimeException("Invalid decision: " + decision);
        }

        request.setStatus(decision);
        request.setHrComment(comment);
        request.setHandledBy(hrUser);
        request.setResolvedAt(LocalDateTime.now());
        return employeeRequestRepository.save(request);
    }

    public EmployeeRequest getById(Long id) {
        return employeeRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    public List<EmployeeRequest> getByEmployee(Long employeeId) {
        return employeeRequestRepository.findByEmployeeId(employeeId);
    }

    public List<EmployeeRequest> getByBranch(Long branchId) {
        return employeeRequestRepository.findByEmployeeBranchId(branchId);
    }

    public List<EmployeeRequest> getAll() {
        return employeeRequestRepository.findAll();
    }

    public List<EmployeeRequest> getEscalated() {
        return employeeRequestRepository.findByStatus("ESCALATED");
    }
}
