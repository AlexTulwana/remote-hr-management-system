package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.service.HearingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/hearings")
public class HearingController {

    private final HearingService hearingService;

    public HearingController(HearingService hearingService) {
        this.hearingService = hearingService;
    }

    @PostMapping
    public ResponseEntity<Hearing> schedule(@RequestBody HearingRequest request) {
        return ResponseEntity.ok(hearingService.schedule(request));
    }

    @PatchMapping("/{id}/outcome")
    public ResponseEntity<Hearing> updateOutcome(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(hearingService.updateOutcome(id, body.get("outcome"), body.get("notes")));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<Hearing> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(hearingService.cancel(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hearing> getById(@PathVariable Long id) {
        return ResponseEntity.ok(hearingService.getById(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Hearing>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(hearingService.getByEmployee(employeeId));
    }

    @GetMapping
    public ResponseEntity<List<Hearing>> getAll() {
        return ResponseEntity.ok(hearingService.getAll());
    }
}