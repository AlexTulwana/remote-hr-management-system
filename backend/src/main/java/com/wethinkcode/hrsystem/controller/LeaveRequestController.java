package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.service.LeaveRequestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<LeaveRequest> submit(
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

        return ResponseEntity.ok(leaveRequestService.submit(employeeId, dto, attachment));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{leaveId}/approve")
    public ResponseEntity<LeaveRequest> approve(@PathVariable Long leaveId) {
        return ResponseEntity.ok(leaveRequestService.approve(leaveId));
    }

    @PreAuthorize("hasRole('MANAGER') or hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{leaveId}/reject")
    public ResponseEntity<LeaveRequest> reject(@PathVariable Long leaveId, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(leaveRequestService.reject(leaveId, body.get("reason")));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<LeaveRequest>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(leaveRequestService.getByEmployee(employeeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<LeaveRequest>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(leaveRequestService.getByBranch(branchId));
    }

    @GetMapping
    public ResponseEntity<List<LeaveRequest>> getAll() {
        return ResponseEntity.ok(leaveRequestService.getAll());
    }
}