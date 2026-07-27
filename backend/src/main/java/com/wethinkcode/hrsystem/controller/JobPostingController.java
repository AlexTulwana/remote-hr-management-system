package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.JobPostingRequest;
import com.wethinkcode.hrsystem.model.JobPosting;
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
    public ResponseEntity<List<JobPosting>> getOpen() {
        return ResponseEntity.ok(jobPostingService.getOpen());
    }

    // PUBLIC - full detail view for a specific posting (the "apply" link target)
    @GetMapping("/{id}")
    public ResponseEntity<JobPosting> getById(@PathVariable Long id) {
        return ResponseEntity.ok(jobPostingService.getById(id));
    }

    // PROTECTED - HR/Admin only, includes closed postings for management
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/all")
    public ResponseEntity<List<JobPosting>> getAll() {
        return ResponseEntity.ok(jobPostingService.getAll());
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<JobPosting> create(@RequestBody JobPostingRequest request) {
        return ResponseEntity.ok(jobPostingService.create(request));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        jobPostingService.delete(id);
        return ResponseEntity.noContent().build();
    }
}