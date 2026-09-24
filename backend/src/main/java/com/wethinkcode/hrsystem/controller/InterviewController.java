package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.InterviewRequest;
import com.wethinkcode.hrsystem.dto.InterviewResponse;
import com.wethinkcode.hrsystem.model.Interview;
import com.wethinkcode.hrsystem.service.InterviewService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<InterviewResponse> schedule(@RequestBody InterviewRequest request) {
        return ResponseEntity.ok(InterviewResponse.from(interviewService.schedule(request)));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<InterviewResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(InterviewResponse.from(interviewService.updateStatus(id, body.get("status"), body.get("notes"))));
    }
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<InterviewResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(InterviewResponse.from(interviewService.getById(id)));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/application/{applicationId}")
    public ResponseEntity<List<InterviewResponse>> getByApplication(@PathVariable Long applicationId) {
        return ResponseEntity.ok(interviewService.getByApplication(applicationId).stream()
                .map(InterviewResponse::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<InterviewResponse>> getAll() {
        return ResponseEntity.ok(interviewService.getAll().stream()
                .map(InterviewResponse::from).toList());
    }
}