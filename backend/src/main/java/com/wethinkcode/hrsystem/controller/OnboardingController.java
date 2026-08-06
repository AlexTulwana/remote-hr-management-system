package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Onboarding;
import com.wethinkcode.hrsystem.service.OnboardingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/onboarding")
@PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/{employeeId}")
    public ResponseEntity<Onboarding> start(@PathVariable Long employeeId, @RequestBody Map<String, String> body) {
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        String notes = body.get("notes");
        return ResponseEntity.ok(onboardingService.start(employeeId, startDate, notes));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<Onboarding> complete(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.complete(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Onboarding> getById(@PathVariable Long id) {
        return ResponseEntity.ok(onboardingService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Onboarding>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(onboardingService.getByEmployee(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<Onboarding>> getAll() {
        return ResponseEntity.ok(onboardingService.getAll());
    }
}