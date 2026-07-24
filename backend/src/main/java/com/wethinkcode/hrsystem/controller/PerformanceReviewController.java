package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.service.PerformanceReviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public ResponseEntity<PerformanceReview> create(@RequestBody PerformanceReviewRequest request) {
        return ResponseEntity.ok(reviewService.create(request));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PerformanceReview>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(reviewService.getByEmployee(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<PerformanceReview>> getAll() {
        return ResponseEntity.ok(reviewService.getAll());
    }
}