package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.ApplicationDocument;
import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.service.ApplicationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeRequest;


import java.util.HashMap;
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
            @RequestParam(required = false) MultipartFile cv,
            @RequestParam Map<String, MultipartFile> allFiles) {

        ApplicationRequest request = new ApplicationRequest();
        request.setCandidateName(candidateName);
        request.setCandidateEmail(candidateEmail);
        request.setCandidatePhone(candidatePhone);
        request.setCoverLetter(coverLetter);

        // Pull out any files whose param name matches a DocumentType (e.g. "ID_COPY", "QUALIFICATION")
        // so extra required documents ride along in the same multipart submission as the CV.
        Map<DocumentType, MultipartFile> additionalDocuments = new HashMap<>();
        for (Map.Entry<String, MultipartFile> entry : allFiles.entrySet()) {
            try {
                DocumentType type = DocumentType.valueOf(entry.getKey());
                if (type != DocumentType.CV) {
                    additionalDocuments.put(type, entry.getValue());
                }
            } catch (IllegalArgumentException ignored) {
                // not a document-type field (e.g. "cv" itself, or unrelated form field) - skip
            }
        }

        return ResponseEntity.ok(applicationService.submit(jobPostingId, request, cv, additionalDocuments));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<ApplicationDocument>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getDocuments(id));
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

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/outcome")
    public ResponseEntity<Application> setOutcome(@PathVariable Long id, @RequestBody ApplicationOutcomeRequest request) {
        return ResponseEntity.ok(applicationService.setOutcome(id, request));
    }
}