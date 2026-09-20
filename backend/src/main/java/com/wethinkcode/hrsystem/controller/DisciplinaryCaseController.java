package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.DisciplinaryCaseRequest;
import com.wethinkcode.hrsystem.dto.DisciplinaryCaseSummary;
import com.wethinkcode.hrsystem.dto.DisciplinaryHistoryEntry;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
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
    public ResponseEntity<DisciplinaryCaseSummary> open(@RequestBody DisciplinaryCaseRequest request,
                                                        Authentication authentication) {
        DisciplinaryCase saved = disciplinaryCaseService.open(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(DisciplinaryCaseSummary.from(saved));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PatchMapping("/{id}/progress")
    public ResponseEntity<DisciplinaryCaseSummary> progress(@PathVariable Long id,
                                                            @RequestBody Map<String, String> body,
                                                            Authentication authentication) {
        Long linkedHearingId = body.get("linkedHearingId") != null
                ? Long.valueOf(body.get("linkedHearingId")) : null;
        DisciplinaryCase updated = disciplinaryCaseService.progressStage(
                id, body.get("stage"), body.get("comment"), linkedHearingId, authentication.getName());
        return ResponseEntity.ok(DisciplinaryCaseSummary.from(updated));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DisciplinaryCaseSummary> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(DisciplinaryCaseSummary.from(
                disciplinaryCaseService.getByIdForViewer(id, authentication.getName())));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<DisciplinaryHistoryEntry>> getHistory(@PathVariable Long id,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(disciplinaryCaseService.getHistoryForViewer(id, authentication.getName())
                .stream().map(DisciplinaryHistoryEntry::from).toList());
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<DisciplinaryCaseSummary>> getByEmployee(@PathVariable Long employeeId,
                                                                       Authentication authentication) {
        return ResponseEntity.ok(disciplinaryCaseService.getByEmployeeForViewer(employeeId, authentication.getName())
                .stream().map(DisciplinaryCaseSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<DisciplinaryCaseSummary>> getByBranch(@PathVariable Long branchId,
                                                                     Authentication authentication) {
        return ResponseEntity.ok(disciplinaryCaseService.getByBranch(branchId, authentication.getName())
                .stream().map(DisciplinaryCaseSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/open")
    public ResponseEntity<List<DisciplinaryCaseSummary>> getAllOpen() {
        return ResponseEntity.ok(disciplinaryCaseService.getAllOpen()
                .stream().map(DisciplinaryCaseSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<DisciplinaryCaseSummary>> getAll() {
        return ResponseEntity.ok(disciplinaryCaseService.getAll()
                .stream().map(DisciplinaryCaseSummary::from).toList());
    }
}
