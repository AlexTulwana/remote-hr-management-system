package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.service.AttendanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/clock-in/{employeeId}")
    public ResponseEntity<Attendance> clockIn(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.clockIn(employeeId));
    }

    @PatchMapping("/clock-out/{attendanceId}")
    public ResponseEntity<Attendance> clockOut(@PathVariable Long attendanceId) {
        return ResponseEntity.ok(attendanceService.clockOut(attendanceId));
    }

    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<Attendance>> getByEmployee(@PathVariable Long employeeId) {
        return ResponseEntity.ok(attendanceService.getByEmployee(employeeId));
    }

    @GetMapping("/branch/{branchId}")
    public ResponseEntity<List<Attendance>> getByBranch(@PathVariable Long branchId) {
        return ResponseEntity.ok(attendanceService.getByBranch(branchId));
    }

    @GetMapping
    public ResponseEntity<List<Attendance>> getAll() {
        return ResponseEntity.ok(attendanceService.getAll());
    }
}
