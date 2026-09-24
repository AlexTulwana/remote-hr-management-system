package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.dto.LeaveRequestSummary;
import com.wethinkcode.hrsystem.service.LeaveRequestService;
import com.wethinkcode.hrsystem.dto.LeaveBalanceResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leave")
public class LeaveRequestController {

    private final LeaveRequestService leaveRequestService;

    public LeaveRequestController(LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @PostMapping(value = "/{employeeId}", consumes = "multipart/form-data")
    public ResponseEntity<LeaveRequestSummary> submit(
            @PathVariable Long employeeId,
            @RequestParam String leaveType,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam String reason,
            @RequestParam(required = false) MultipartFile attachment) {

        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType(leaveType);
        dto.setStartDate(java.time.LocalDate.parse(startDate));
        dto.setEndDate(java.time.LocalDate.parse(endDate));
        dto.setReason(reason);

        return ResponseEntity.ok(LeaveRequestSummary.from(leaveRequestService.submit(employeeId, dto, attachment)));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{leaveId}/approve")
    public ResponseEntity<LeaveRequestSummary> approve(@PathVariable Long leaveId) {
        return ResponseEntity.ok(LeaveRequestSummary.from(leaveRequestService.approve(leaveId)));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{leaveId}/reject")
    public ResponseEntity<LeaveRequestSummary> reject(@PathVariable Long leaveId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(LeaveRequestSummary.from(leaveRequestService.reject(leaveId, body.get("reason"))));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequestSummary>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getByEmployee(employeeId).stream()
                .map(LeaveRequestSummary::from).toList());
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<LeaveRequestSummary>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(leaveRequestService.getByBranch(branchId).stream()
                .map(LeaveRequestSummary::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<LeaveRequestSummary>> getAll() {
        return ResponseEntity.ok(leaveRequestService.getAll().stream()
                .map(LeaveRequestSummary::from).toList());
    }

    @GetMapping("/{leaveId}/attachment")
    public ResponseEntity<Resource> getAttachment(@PathVariable Long leaveId) {
        Path path = leaveRequestService.getAttachmentPath(leaveId);
        String filename = path.getFileName().toString();
        String displayName = filename.contains("_") ? filename.substring(filename.indexOf('_') + 1) : filename;
        displayName = displayName.replaceAll("[\"\\r\\n]", "");
        return ResponseEntity.ok()
                .contentType(resolveMediaType(filename))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + displayName + "\"")
                .body(new FileSystemResource(path));
    }

    @GetMapping("/balance/{employeeId}")
    public ResponseEntity<LeaveBalanceResponse> getBalance(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getBalance(employeeId));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/balances")
    public ResponseEntity<List<LeaveBalanceResponse>> getAllBalances() {
        return ResponseEntity.ok(leaveRequestService.getAllBalances());
    }

    private static MediaType resolveMediaType(String filename) {
        String name = filename.toLowerCase();
        if (name.endsWith(".pdf")) return MediaType.APPLICATION_PDF;
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return MediaType.IMAGE_JPEG;
        if (name.endsWith(".png")) return MediaType.IMAGE_PNG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
