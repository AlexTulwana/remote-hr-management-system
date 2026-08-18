package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.RecruitmentAnalyticsSummary;
import com.wethinkcode.hrsystem.service.RecruitmentAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/recruitment")
public class RecruitmentAnalyticsController {

    private final RecruitmentAnalyticsService recruitmentAnalyticsService;

    public RecruitmentAnalyticsController(RecruitmentAnalyticsService recruitmentAnalyticsService) {
        this.recruitmentAnalyticsService = recruitmentAnalyticsService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<RecruitmentAnalyticsSummary> getSummary(
            @RequestParam(defaultValue = "30") int periodDays) {
        return ResponseEntity.ok(recruitmentAnalyticsService.getSummary(periodDays));
    }
}