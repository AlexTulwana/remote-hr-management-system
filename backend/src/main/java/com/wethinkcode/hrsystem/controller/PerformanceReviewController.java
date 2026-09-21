package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.dto.PerformanceReviewSummary;
import com.wethinkcode.hrsystem.service.PerformanceReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
public class PerformanceReviewController {

    private final PerformanceReviewService reviewService;

    public PerformanceReviewController(PerformanceReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<PerformanceReviewSummary> create(@RequestBody PerformanceReviewRequest request,
                                                           Authentication authentication) {
        return ResponseEntity.ok(PerformanceReviewSummary.from(
                reviewService.create(request, authentication.getName())));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PerformanceReviewSummary>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(reviewService.getByEmployee(employeeId).stream()
                .map(PerformanceReviewSummary::from).toList());
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<PerformanceReviewSummary>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(reviewService.getByBranch(branchId).stream()
                .map(PerformanceReviewSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PerformanceReviewSummary>> getAll() {
        return ResponseEntity.ok(reviewService.getAll().stream()
                .map(PerformanceReviewSummary::from).toList());
    }
}
