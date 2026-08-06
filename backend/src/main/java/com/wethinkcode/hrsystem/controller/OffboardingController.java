package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Offboarding;
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
    public ResponseEntity<Offboarding> start(@PathVariable Long employeeId, @RequestBody Map<String, String> body) {
        String type = body.get("type");
        LocalDate effectiveDate = LocalDate.parse(body.get("effectiveDate"));
        String reason = body.get("reason");
        return ResponseEntity.ok(offboardingService.start(employeeId, type, effectiveDate, reason));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Offboarding> complete(@PathVariable Long id) {
        return ResponseEntity.ok(offboardingService.complete(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Offboarding> getById(@PathVariable Long id) {
        return ResponseEntity.ok(offboardingService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Offboarding>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(offboardingService.getByEmployee(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<Offboarding>> getAll() {
        return ResponseEntity.ok(offboardingService.getAll());
    }
}