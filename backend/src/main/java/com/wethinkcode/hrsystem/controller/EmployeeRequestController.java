package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeRequestSubmission;
import com.wethinkcode.hrsystem.dto.EmployeeRequestSummary;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
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

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_MANAGER")
                        || a.getAuthority().equals("ROLE_HR")
                        || a.getAuthority().equals("ROLE_ADMIN"));
    }

    private EmployeeRequestSummary toSummary(EmployeeRequest request, Authentication authentication) {
        return isStaff(authentication)
                ? EmployeeRequestSummary.forStaff(request)
                : EmployeeRequestSummary.from(request);
    }

    @PostMapping
    public ResponseEntity<EmployeeRequestSummary> submit(@RequestBody EmployeeRequestSubmission submission,
                                                    Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(EmployeeRequestSummary.from(employeeRequestService.submit(submission, authentication.getName())));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @PatchMapping("/{id}/manager-decision")
    public ResponseEntity<EmployeeRequestSummary> managerDecision(@PathVariable Long id,
                                                             @RequestBody Map<String, String> body,
                                                             Authentication authentication) {
        return ResponseEntity.ok(EmployeeRequestSummary.forStaff(employeeRequestService.managerDecision(
                id, body.get("decision"), body.get("comment"), authentication.getName())));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/hr-decision")
    public ResponseEntity<EmployeeRequestSummary> hrDecision(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication authentication) {
        return ResponseEntity.ok(EmployeeRequestSummary.forStaff(employeeRequestService.hrDecision(
                id, body.get("decision"), body.get("comment"), authentication.getName())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EmployeeRequestSummary> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toSummary(employeeRequestService.getById(id, authentication.getName()), authentication));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeRequestSummary>> getByEmployee(@PathVariable Long employeeId, Authentication authentication) {
        return ResponseEntity.ok(employeeRequestService.getByEmployee(employeeId, authentication.getName()).stream()
                .map(r -> toSummary(r, authentication)).toList());
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<EmployeeRequestSummary>> getByBranch(@PathVariable Long branchId,
                                                                    Authentication authentication) {
        return ResponseEntity.ok(employeeRequestService.getByBranch(branchId, authentication.getName()).stream()
                .map(EmployeeRequestSummary::forStaff).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<EmployeeRequestSummary>> getAll() {
        return ResponseEntity.ok(employeeRequestService.getAll().stream()
                .map(EmployeeRequestSummary::forStaff).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/escalated")
    public ResponseEntity<List<EmployeeRequestSummary>> getEscalated() {
        return ResponseEntity.ok(employeeRequestService.getEscalated().stream()
                .map(EmployeeRequestSummary::forStaff).toList());
    }
}
