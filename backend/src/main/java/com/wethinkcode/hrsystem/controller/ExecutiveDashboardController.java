package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.BranchComparisonEntry;
import com.wethinkcode.hrsystem.dto.ExecutiveKpiSummary;
import com.wethinkcode.hrsystem.dto.TurnoverTrendPoint;
import com.wethinkcode.hrsystem.service.ExecutiveDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/executive")
@PreAuthorize("hasRole('ADMIN')")
public class ExecutiveDashboardController {

    private final ExecutiveDashboardService executiveDashboardService;

    public ExecutiveDashboardController(ExecutiveDashboardService executiveDashboardService) {
        this.executiveDashboardService = executiveDashboardService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<ExecutiveKpiSummary> getKpis() {
        return ResponseEntity.ok(executiveDashboardService.getKpis());
    }

    @GetMapping("/branch-comparison")
    public ResponseEntity<List<BranchComparisonEntry>> getBranchComparison() {
        return ResponseEntity.ok(executiveDashboardService.getBranchComparison());
    }

    @GetMapping("/turnover-trend")
    public ResponseEntity<List<TurnoverTrendPoint>> getTurnoverTrend(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(executiveDashboardService.getTurnoverTrend(days));
    }
}