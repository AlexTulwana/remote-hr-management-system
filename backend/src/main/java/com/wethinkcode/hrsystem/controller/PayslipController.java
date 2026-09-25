package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.BulkPayslipUploadResult;
import com.wethinkcode.hrsystem.dto.PayslipSummary;
import com.wethinkcode.hrsystem.service.PayslipService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;


import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payslips")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<PayslipSummary>> getAll() {
        return ResponseEntity.ok(payslipService.getAll().stream().map(PayslipSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping(value = "/bulk", consumes = "multipart/form-data")
    public ResponseEntity<List<BulkPayslipUploadResult>> bulkUpload(
            @RequestParam String payPeriod,
            @RequestParam MultipartFile[] files) {
        return ResponseEntity.ok(payslipService.bulkUpload(payPeriod, files));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping(value = "/{employeeId}", consumes = "multipart/form-data")
    public ResponseEntity<PayslipSummary> upload(
            @PathVariable Long employeeId,
            @RequestParam String payPeriod,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(PayslipSummary.from(payslipService.upload(employeeId, payPeriod, file)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<PayslipSummary>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payslipService.getByEmployee(employeeId).stream()
                .map(PayslipSummary::from).toList());
    }

    @GetMapping("/{payslipId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long payslipId) {
        Resource file = payslipService.downloadFile(payslipId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }

    @PostMapping("/{payslipId}/email")
    public ResponseEntity<Map<String, String>> emailToSelf(@PathVariable Long payslipId) {
        payslipService.emailToSelf(payslipId);
        return ResponseEntity.ok(Map.of("status", "sent"));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @DeleteMapping("/{payslipId}")
    public ResponseEntity<Void> delete(@PathVariable Long payslipId) {
        payslipService.delete(payslipId);
        return ResponseEntity.noContent().build();
    }
}
