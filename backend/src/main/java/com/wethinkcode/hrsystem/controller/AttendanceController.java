package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Attendance;
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
    public ResponseEntity<Attendance> clockIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.clockIn(employeeId));
    }

    // Self, or MANAGER (same branch)/HR/ADMIN - enforced in AttendanceService
    @PatchMapping("/clock-out/{attendanceId}")
    public ResponseEntity<Attendance> clockOut(@PathVariable Long attendanceId) {
        return ResponseEntity.ok(attendanceService.clockOut(attendanceId));
    }

    // Self, or MANAGER (same branch)/HR/ADMIN - enforced in AttendanceService
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Attendance>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.getByEmployee(employeeId));
    }

    // MANAGER (own branch only)/HR/ADMIN - enforced in AttendanceService
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<Attendance>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(attendanceService.getByBranch(branchId));
    }

    // HR/ADMIN only
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<Attendance>> getAll() {
        return ResponseEntity.ok(attendanceService.getAll());
    }
}