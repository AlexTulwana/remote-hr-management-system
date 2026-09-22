package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EscalationRequest;
import com.wethinkcode.hrsystem.dto.EscalationResponse;
import com.wethinkcode.hrsystem.service.EscalationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    // Any authenticated user (Manager or Employee) can submit - explicit annotation for clarity,
    // in addition to the default authenticated() rule in SecurityConfig
    @PreAuthorize("isAuthenticated()")
    @PostMapping
    public ResponseEntity<EscalationResponse> submit(@RequestBody EscalationRequest request) {
        return ResponseEntity.ok(EscalationResponse.from(escalationService.submit(request)));
    }

    // HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<EscalationResponse>> getAll() {
        return ResponseEntity.ok(escalationService.getAll().stream().map(EscalationResponse::from).toList());
    }

    // HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/link-hearing/{hearingId}")
    public ResponseEntity<EscalationResponse> linkToHearing(@PathVariable Long id, @PathVariable Long hearingId) {
        return ResponseEntity.ok(EscalationResponse.from(escalationService.linkToHearing(id, hearingId)));
    }

    // HR/Admin only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<EscalationResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(EscalationResponse.from(escalationService.updateStatus(id, body.get("status"))));
    }
}
