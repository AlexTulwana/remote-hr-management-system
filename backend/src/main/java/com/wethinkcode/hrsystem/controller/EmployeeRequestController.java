package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeRequestSubmission;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.service.EmployeeRequestService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/employee-requests")
public class EmployeeRequestController {

    private final EmployeeRequestService employeeRequestService;
    private final UserRepository userRepository;

    public EmployeeRequestController(EmployeeRequestService employeeRequestService,
                                      UserRepository userRepository) {
        this.employeeRequestService = employeeRequestService;
        this.userRepository = userRepository;
    }

    @PostMapping
    public ResponseEntity<EmployeeRequest> submit(@RequestBody EmployeeRequestSubmission submission) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeRequestService.submit(submission));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{id}/manager-decision")
    public ResponseEntity<EmployeeRequest> managerDecision(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body,
                                                             Authentication authentication) {
        EmployeeRequest updated = employeeRequestService.managerDecision(
                id, body.get("decision"), body.get("comment"), authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/hr-decision")
    public ResponseEntity<EmployeeRequest> hrDecision(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication authentication) {
        EmployeeRequest updated = employeeRequestService.hrDecision(
                id, body.get("decision"), body.get("comment"), authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeRequest> getById(@PathVariable Long id) {
        return ResponseEntity.ok(employeeRequestService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeRequest>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeRequestService.getByEmployee(employeeId));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<EmployeeRequest>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(employeeRequestService.getByBranch(branchId));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<EmployeeRequest>> getAll() {
        return ResponseEntity.ok(employeeRequestService.getAll());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/escalated")
    public ResponseEntity<List<EmployeeRequest>> getEscalated() {
        return ResponseEntity.ok(employeeRequestService.getEscalated());
    }
}
