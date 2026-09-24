package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.OnboardingResponse;
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
    public ResponseEntity<OnboardingResponse> start(@PathVariable Long employeeId, @RequestBody Map<String, String> body) {
        LocalDate startDate = LocalDate.parse(body.get("startDate"));
        String notes = body.get("notes");
        return ResponseEntity.ok(OnboardingResponse.from(onboardingService.start(employeeId, startDate, notes)));
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<OnboardingResponse> complete(@PathVariable Long id) {
        return ResponseEntity.ok(OnboardingResponse.from(onboardingService.complete(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OnboardingResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(OnboardingResponse.from(onboardingService.getById(id)));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<OnboardingResponse>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(onboardingService.getByEmployee(employeeId).stream().map(OnboardingResponse::from).toList());
    }

    @GetMapping
    public ResponseEntity<List<OnboardingResponse>> getAll() {
        return ResponseEntity.ok(onboardingService.getAll().stream().map(OnboardingResponse::from).toList());
    }
}