package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.DashboardStats;
import com.wethinkcode.hrsystem.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // Role-based scoping (HR/Admin: full stats, Manager: own branch, Employee: denied)
    // happens in DashboardService - explicit annotation here for clarity, in addition
    // to the default authenticated() rule in SecurityConfig
    @PreAuthorize("isAuthenticated()")
    @GetMapping
    public ResponseEntity<DashboardStats> getStats() {
        return ResponseEntity.ok(dashboardService.getStats());
    }
}