package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EscalationRequest;
import com.wethinkcode.hrsystem.model.Escalation;
import com.wethinkcode.hrsystem.service.EscalationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/escalations")
public class EscalationController {

    private final EscalationService escalationService;

    public EscalationController(EscalationService escalationService) {
        this.escalationService = escalationService;
    }

    // Any authenticated user (Manager or Employee) can submit
    @PostMapping
    public ResponseEntity<Escalation> submit(@RequestBody EscalationRequest request) {
        return ResponseEntity.ok(escalationService.submit(request));
    }

    // HR/Admin only - full proper role enforcement comes in Phase 16
    @GetMapping
    public ResponseEntity<List<Escalation>> getAll() {
        return ResponseEntity.ok(escalationService.getAll());
    }

    // HR/Admin only
    @PatchMapping("/{id}/link-hearing/{hearingId}")
    public ResponseEntity<Escalation> linkToHearing(@PathVariable Long id, @PathVariable Long hearingId) {
        return ResponseEntity.ok(escalationService.linkToHearing(id, hearingId));
    }

    // HR/Admin only
    @PatchMapping("/{id}/status")
    public ResponseEntity<Escalation> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(escalationService.updateStatus(id, body.get("status")));
    }
}