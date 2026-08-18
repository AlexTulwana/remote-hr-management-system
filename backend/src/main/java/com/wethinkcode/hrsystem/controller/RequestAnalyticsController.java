package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.RequestAnalyticsSummary;
import com.wethinkcode.hrsystem.service.RequestAnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analytics/requests")
public class RequestAnalyticsController {

    private final RequestAnalyticsService requestAnalyticsService;

    public RequestAnalyticsController(RequestAnalyticsService requestAnalyticsService) {
        this.requestAnalyticsService = requestAnalyticsService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<RequestAnalyticsSummary> getSummary(
            @RequestParam(defaultValue = "30") int periodDays) {
        return ResponseEntity.ok(requestAnalyticsService.getSummary(periodDays));
    }
}