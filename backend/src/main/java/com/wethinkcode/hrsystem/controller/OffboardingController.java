package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.OffboardingResponse;
import com.wethinkcode.hrsystem.service.OffboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offboarding")
@PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
public class OffboardingController {

    private final OffboardingService offboardingService;

    public OffboardingController(OffboardingService offboardingService) {
        this.offboardingService = offboardingService;
    }

    @PostMapping("/{employeeId}")
    public ResponseEntity<OffboardingResponse> start(@PathVariable Long employeeId, @RequestBody Map<String, String> body) {
        String type = body.get("type");
        LocalDate effectiveDate = LocalDate.parse(body.get("effectiveDate"));
        String reason = body.get("reason");
        return ResponseEntity.ok(OffboardingResponse.from(offboardingService.start(employeeId, type, effectiveDate, reason)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<OffboardingResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(OffboardingResponse.from(offboardingService.complete(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OffboardingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(OffboardingResponse.from(offboardingService.getById(id)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<OffboardingResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(offboardingService.getByEmployee(employeeId).stream().map(OffboardingResponse::from).toList());
    }

    @GetMapping
    public ResponseEntity<List<OffboardingResponse>> getAll() {
        return ResponseEntity.ok(offboardingService.getAll().stream().map(OffboardingResponse::from).toList());
    }
}