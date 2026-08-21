package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.service.HrDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard/hr")
@PreAuthorize("hasAnyRole('HR','ADMIN')")
public class HrDashboardController {

    private final HrDashboardService hrDashboardService;

    public HrDashboardController(HrDashboardService hrDashboardService) {
        this.hrDashboardService = hrDashboardService;
    }

    @GetMapping("/headcount")
    public ResponseEntity<ManagerHeadcountSummary> getHeadcount() {
        return ResponseEntity.ok(hrDashboardService.getHeadcount());
    }

    @GetMapping("/headcount-trend")
    public ResponseEntity<List<HeadcountTrendPoint>> getHeadcountTrend(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(hrDashboardService.getHeadcountTrend(days));
    }

    @GetMapping("/recruitment-funnel")
    public ResponseEntity<RecruitmentFunnelSummary> getRecruitmentFunnel() {
        return ResponseEntity.ok(hrDashboardService.getRecruitmentFunnel());
    }

    @GetMapping("/pending-items")
    public ResponseEntity<HrPendingItemsSummary> getPendingItems() {
        return ResponseEntity.ok(hrDashboardService.getPendingItems());
    }

    @GetMapping("/turnover")
    public ResponseEntity<TurnoverSummary> getTurnover() {
        return ResponseEntity.ok(hrDashboardService.getTurnover());
    }

    @GetMapping("/salary-summary")
    public ResponseEntity<HrSalarySummary> getSalarySummary() {
        return ResponseEntity.ok(hrDashboardService.getSalarySummary());
    }

    @GetMapping("/salary-trend")
    public ResponseEntity<List<SalaryTrendPoint>> getSalaryTrend(
            @RequestParam(defaultValue = "90") int days) {
        return ResponseEntity.ok(hrDashboardService.getSalaryTrend(days));
    }
}