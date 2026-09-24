package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.ApplicationRequest;
import com.wethinkcode.hrsystem.dto.ApplicationResponse;
import com.wethinkcode.hrsystem.dto.ApplicationDocumentResponse;
import com.wethinkcode.hrsystem.dto.ApplicationSubmitResponse;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.service.ApplicationService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    public ResponseEntity<ApplicationSubmitResponse> submit(
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

        Application saved = applicationService.submit(jobPostingId, request, cv, additionalDocuments);
        return ResponseEntity.ok(ApplicationSubmitResponse.from(saved));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}/documents")
    public ResponseEntity<List<ApplicationDocumentResponse>> getDocuments(@PathVariable Long id) {
        return ResponseEntity.ok(applicationService.getDocuments(id).stream()
                .map(ApplicationDocumentResponse::from).toList());
    }

    // PROTECTED - HR/Admin only. Content-Type resolved from file extension so PDFs/images
    // can render inline in a browser tab; Word docs fall back to a plain download.
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}/cv")
    public ResponseEntity<Resource> downloadCv(@PathVariable Long id) {
        Resource file = applicationService.downloadCv(id);
        MediaType contentType = applicationService.resolveContentType(file.getFilename());
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionFor(contentType, file.getFilename()))
                .body(file);
    }

    // PROTECTED - HR/Admin only. Same Content-Type resolution as the CV download.
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}/documents/{documentId}/download")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id, @PathVariable Long documentId) {
        Resource file = applicationService.downloadDocument(id, documentId);
        MediaType contentType = applicationService.resolveContentType(file.getFilename());
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, dispositionFor(contentType, file.getFilename()))
                .body(file);
    }

    private String dispositionFor(MediaType contentType, String filename) {
        boolean viewable = contentType.equals(MediaType.APPLICATION_PDF)
                || contentType.equals(MediaType.IMAGE_JPEG)
                || contentType.equals(MediaType.IMAGE_PNG);
        String disposition = viewable ? "inline" : "attachment";
        return disposition + "; filename=\"" + filename + "\"";
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApplicationResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.updateStatus(id, body.get("status"))));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/job-posting/{jobPostingId}")
    public ResponseEntity<List<ApplicationResponse>> getByJobPosting(@PathVariable Long jobPostingId) {
        return ResponseEntity.ok(applicationService.getByJobPosting(jobPostingId).stream()
                .map(ApplicationResponse::from).toList());
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ApplicationResponse>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(applicationService.getByStatus(status).stream()
                .map(ApplicationResponse::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<ApplicationResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.getById(id)));
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<ApplicationResponse>> getAll() {
        return ResponseEntity.ok(applicationService.getAll().stream()
                .map(ApplicationResponse::from).toList());
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/outcome")
    public ResponseEntity<ApplicationResponse> setOutcome(@PathVariable Long id, @RequestBody ApplicationOutcomeRequest request) {
        return ResponseEntity.ok(ApplicationResponse.from(applicationService.setOutcome(id, request)));
    }
}