package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.AttendanceSummary;
import com.wethinkcode.hrsystem.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // Self, or MANAGER (same branch)/HR/ADMIN - enforced in AttendanceService
    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<AttendanceSummary> clockIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(AttendanceSummary.from(attendanceService.clockIn(employeeId)));
    }

    // Self, or MANAGER (same branch)/HR/ADMIN - enforced in AttendanceService
    @PatchMapping("/clock-out/{attendanceId}")
    public ResponseEntity<AttendanceSummary> clockOut(@PathVariable Long attendanceId) {
        return ResponseEntity.ok(AttendanceSummary.from(attendanceService.clockOut(attendanceId)));
    }

    // Self, or MANAGER (same branch)/HR/ADMIN - enforced in AttendanceService
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<AttendanceSummary>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.getByEmployee(employeeId).stream()
                .map(AttendanceSummary::from).toList());
    }

    // MANAGER (own branch only)/HR/ADMIN - enforced in AttendanceService
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<AttendanceSummary>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(attendanceService.getByBranch(branchId).stream()
                .map(AttendanceSummary::from).toList());
    }

    // HR/ADMIN only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<AttendanceSummary>> getAll() {
        return ResponseEntity.ok(attendanceService.getAll().stream()
                .map(AttendanceSummary::from).toList());
    }
}
