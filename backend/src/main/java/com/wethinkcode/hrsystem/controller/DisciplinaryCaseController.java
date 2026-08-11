package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.DisciplinaryCaseRequest;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import com.wethinkcode.hrsystem.model.DisciplinaryCaseHistory;
import com.wethinkcode.hrsystem.service.DisciplinaryCaseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disciplinary-cases")
public class DisciplinaryCaseController {

    private final DisciplinaryCaseService disciplinaryCaseService;

    public DisciplinaryCaseController(DisciplinaryCaseService disciplinaryCaseService) {
        this.disciplinaryCaseService = disciplinaryCaseService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping
    public ResponseEntity<DisciplinaryCase> open(@RequestBody DisciplinaryCaseRequest request,
                                                   Authentication authentication) {
        DisciplinaryCase saved = disciplinaryCaseService.open(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PatchMapping("/{id}/progress")
    public ResponseEntity<DisciplinaryCase> progress(@PathVariable Long id,
                                                        @RequestBody Map<String, String> body,
                                                        Authentication authentication) {
        Long linkedHearingId = body.get("linkedHearingId") != null
                ? Long.valueOf(body.get("linkedHearingId")) : null;
        DisciplinaryCase updated = disciplinaryCaseService.progressStage(
                id, body.get("stage"), body.get("comment"), linkedHearingId, authentication.getName());
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaryCase> getById(@PathVariable Long id) {
        return ResponseEntity.ok(disciplinaryCaseService.getById(id));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DisciplinaryCaseHistory>> getHistory(@PathVariable Long id) {
        return ResponseEntity.ok(disciplinaryCaseService.getHistory(id));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<DisciplinaryCase>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(disciplinaryCaseService.getByEmployee(employeeId));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<DisciplinaryCase>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(disciplinaryCaseService.getByBranch(branchId));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/open")
    public ResponseEntity<List<DisciplinaryCase>> getAllOpen() {
        return ResponseEntity.ok(disciplinaryCaseService.getAllOpen());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DisciplinaryCase>> getAll() {
        return ResponseEntity.ok(disciplinaryCaseService.getAll());
    }
}
