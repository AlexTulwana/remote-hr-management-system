package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDashboardSummary;
import com.wethinkcode.hrsystem.service.EmployeeDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard/employee")
@PreAuthorize("hasRole('EMPLOYEE')")
public class EmployeeDashboardController {

    private final EmployeeDashboardService employeeDashboardService;

    public EmployeeDashboardController(EmployeeDashboardService employeeDashboardService) {
        this.employeeDashboardService = employeeDashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<EmployeeDashboardSummary> getSummary() {
        return ResponseEntity.ok(employeeDashboardService.getSummary());
    }
}