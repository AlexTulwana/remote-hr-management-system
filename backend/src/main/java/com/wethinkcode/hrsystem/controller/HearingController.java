package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.HearingParticipantRequest;
import com.wethinkcode.hrsystem.dto.HearingParticipantSummary;
import com.wethinkcode.hrsystem.dto.HearingRequest;
import com.wethinkcode.hrsystem.dto.HearingSummary;
import com.wethinkcode.hrsystem.model.Hearing;
import com.wethinkcode.hrsystem.service.HearingService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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

    private boolean isStaff(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_HR") || a.getAuthority().equals("ROLE_ADMIN"));
    }

    private HearingSummary toSummary(Hearing hearing, Authentication authentication) {
        return isStaff(authentication) ? HearingSummary.forStaff(hearing) : HearingSummary.from(hearing);
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<HearingSummary> schedule(@RequestBody HearingRequest request, Authentication authentication) {
        return ResponseEntity.ok(HearingSummary.forStaff(hearingService.schedule(request, authentication.getName())));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/outcome")
    public ResponseEntity<HearingSummary> updateOutcome(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(HearingSummary.forStaff(
                hearingService.updateOutcome(id, body.get("outcome"), body.get("notes"))));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<HearingSummary> cancel(@PathVariable Long id) {
        return ResponseEntity.ok(HearingSummary.forStaff(hearingService.cancel(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<HearingSummary> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(toSummary(hearingService.getById(id), authentication));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<HearingSummary>> getByEmployee(@PathVariable Long employeeId,
                                                              Authentication authentication) {
        return ResponseEntity.ok(hearingService.getByEmployee(employeeId).stream()
                .map(h -> toSummary(h, authentication)).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<HearingSummary>> getAll() {
        return ResponseEntity.ok(hearingService.getAll().stream().map(HearingSummary::forStaff).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping("/{hearingId}/participants")
    public ResponseEntity<HearingParticipantSummary> addParticipant(
            @PathVariable Long hearingId, @RequestBody HearingParticipantRequest request) {
        return ResponseEntity.ok(HearingParticipantSummary.from(hearingService.addParticipant(hearingId, request)));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{hearingId}/participants")
    public ResponseEntity<List<HearingParticipantSummary>> getParticipants(@PathVariable Long hearingId) {
        return ResponseEntity.ok(hearingService.getParticipants(hearingId).stream()
                .map(HearingParticipantSummary::from).toList());
    }
}
