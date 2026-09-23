package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmailTemplateUpdateRequest;
import com.wethinkcode.hrsystem.dto.JobPostingPublicResponse;
import com.wethinkcode.hrsystem.dto.JobPostingRequest;
import com.wethinkcode.hrsystem.dto.JobPostingResponse;
import com.wethinkcode.hrsystem.service.JobPostingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/job-postings")
public class JobPostingController {

    private final JobPostingService jobPostingService;

    public JobPostingController(JobPostingService jobPostingService) {
        this.jobPostingService = jobPostingService;
    }

    // PUBLIC - candidates browsing open positions
    @GetMapping
    public ResponseEntity<List<JobPostingPublicResponse>> getOpen() {
        return ResponseEntity.ok(jobPostingService.getOpen().stream().map(JobPostingPublicResponse::from).toList());
    }

    // PUBLIC - full detail view for a specific posting (the "apply" link target)
    @GetMapping("/{id}")
    public ResponseEntity<JobPostingPublicResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(JobPostingPublicResponse.from(jobPostingService.getById(id)));
    }

    // PROTECTED - HR/Admin only, includes closed postings for management
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<JobPostingResponse>> getAll() {
        return ResponseEntity.ok(jobPostingService.getAll().stream().map(JobPostingResponse::from).toList());
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<JobPostingResponse> create(@RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.create(request)));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<JobPostingResponse> update(@PathVariable Long id, @RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.update(id, request)));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/email-templates")
    public ResponseEntity<JobPostingResponse> updateEmailTemplates(@PathVariable Long id, @RequestBody EmailTemplateUpdateRequest request) {
        return ResponseEntity.ok(JobPostingResponse.from(jobPostingService.updateEmailTemplates(id, request)));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
