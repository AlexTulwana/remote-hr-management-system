package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    // PUBLIC - candidates apply directly, no login needed
    @PostMapping(value = "/{jobPostingId}", consumes = "multipart/form-data")
    public ResponseEntity<Application> submit(
            @PathVariable Long jobPostingId,
            @RequestParam String candidateName,
            @RequestParam String candidateEmail,
            @RequestParam(required = false) String candidatePhone,
            @RequestParam(required = false) String coverLetter,
            @RequestParam(required = false) MultipartFile cv) {

        ApplicationRequest request = new ApplicationRequest();
        request.setCandidateName(candidateName);
        request.setCandidateEmail(candidateEmail);
        request.setCandidatePhone(candidatePhone);
        request.setCoverLetter(coverLetter);

        return ResponseEntity.ok(applicationService.submit(jobPostingId, request, cv));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<Application> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(applicationService.updateStatus(id, body.get("status")));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/job-posting/{jobPostingId}")
    public ResponseEntity<List<Application>> getByJobPosting(@PathVariable Long jobPostingId) {
        return ResponseEntity.ok(applicationService.getByJobPosting(jobPostingId));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<Application>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(applicationService.getByStatus(status));
    }
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<Application> getById(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getById(id));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Application>> getAll() {
        return ResponseEntity.ok(applicationService.getAll());
    }
}