package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Payslip;
import com.wethinkcode.hrsystem.service.PayslipService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/payslips")
public class PayslipController {

    private final PayslipService payslipService;

    public PayslipController(PayslipService payslipService) {
        this.payslipService = payslipService;
    }

    @PostMapping(value = "/{employeeId}", consumes = "multipart/form-data")
    public ResponseEntity<Payslip> upload(
            @PathVariable Long employeeId,
            @RequestParam String payPeriod,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(payslipService.upload(employeeId, payPeriod, file));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Payslip>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(payslipService.getByEmployee(employeeId));
    }

    @GetMapping("/{payslipId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long payslipId) {
        Resource file = payslipService.downloadFile(payslipId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"")
                .body(file);
    }
}