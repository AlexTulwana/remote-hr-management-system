package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDocumentResponse;
import com.wethinkcode.hrsystem.model.DocumentType;
import com.wethinkcode.hrsystem.model.EmployeeDocument;
import com.wethinkcode.hrsystem.service.EmployeeDocumentService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeDocumentController {

    private final EmployeeDocumentService documentService;

    public EmployeeDocumentController(EmployeeDocumentService documentService) {
        this.documentService = documentService;
    }

    // Access checked in service: HR/Admin may upload any type for any employee;
    // an employee/manager may self-upload permitted types for their own record.
    @PostMapping(value = "/{employeeId}/documents", consumes = "multipart/form-data")
    public ResponseEntity<EmployeeDocumentResponse> upload(@PathVariable Long employeeId,
                                                   @RequestParam MultipartFile file,
                                                   @RequestParam DocumentType documentType,
                                                   @RequestParam(required = false) String description,
                                                   Authentication authentication) {
        EmployeeDocument doc = documentService.upload(employeeId, file, documentType, description,
                authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(EmployeeDocumentResponse.from(doc));
    }

    // PROTECTED - HR/Admin see any employee's docs, employee sees only their own
    @GetMapping("/{employeeId}/documents")
    public ResponseEntity<List<EmployeeDocumentResponse>> list(@PathVariable Long employeeId,
                                                       Authentication authentication) {
        List<EmployeeDocumentResponse> docs = documentService.list(employeeId, authentication.getName())
                .stream()
                .map(EmployeeDocumentResponse::from)
                .toList();
        return ResponseEntity.ok(docs);
    }

    @GetMapping("/documents/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) throws IOException {
        EmployeeDocument doc = documentService.get(id, authentication.getName());
        Resource resource = new UrlResource(Paths.get(doc.getFilePath()).toUri());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(resource);
    }

    // PROTECTED - HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        documentService.softDelete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
