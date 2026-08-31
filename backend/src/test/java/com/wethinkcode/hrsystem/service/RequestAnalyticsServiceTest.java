package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.RequestAnalyticsSummary;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestAnalyticsServiceTest {

    @Mock
    private EmployeeRequestRepository requestRepository;

    @InjectMocks
    private RequestAnalyticsService requestAnalyticsService;

    private EmployeeRequest request(String status, LocalDateTime submittedAt, LocalDateTime resolvedAt) {
        EmployeeRequest req = new EmployeeRequest();
        req.setStatus(status);
        req.setSubmittedAt(submittedAt);
        req.setResolvedAt(resolvedAt);
        return req;
    }

    @Test
    void getSummary_noRequests_returnsZeroedSummary() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of());

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(30, summary.getPeriodDays());
        assertEquals(0, summary.getTotalResolved());
        assertEquals(0.0, summary.getApprovalRate());
        assertEquals(0.0, summary.getEscalationRate());
        assertEquals(0.0, summary.getAvgResolutionHours());
    }

    @Test
    void getSummary_countsByStatus() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of(
                request("APPROVED", null, null),
                request("APPROVED", null, null),
                request("REJECTED", null, null),
                request("ESCALATED", null, null)));

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(4, summary.getTotalResolved());
        assertEquals(2, summary.getApproved());
        assertEquals(1, summary.getRejected());
        assertEquals(1, summary.getEscalated());
    }

    @Test
    void getSummary_approvalRate_computedAgainstTotal() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of(
                request("APPROVED", null, null),
                request("APPROVED", null, null),
                request("APPROVED", null, null),
                request("REJECTED", null, null)));

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(0.75, summary.getApprovalRate());
    }

    @Test
    void getSummary_escalationRate_computedAgainstTotal() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of(
                request("ESCALATED", null, null),
                request("APPROVED", null, null),
                request("REJECTED", null, null),
                request("REJECTED", null, null)));

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(0.25, summary.getEscalationRate());
    }

    @Test
    void getSummary_avgResolutionHours_computedFromTimestamps() {
        LocalDateTime submitted1 = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime resolved1 = LocalDateTime.of(2026, 1, 2, 9, 0); // 24 hours

        LocalDateTime submitted2 = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime resolved2 = LocalDateTime.of(2026, 1, 1, 15, 0); // 6 hours

        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of(
                request("APPROVED", submitted1, resolved1),
                request("REJECTED", submitted2, resolved2)));

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(15.0, summary.getAvgResolutionHours()); // (24 + 6) / 2
    }

    @Test
    void getSummary_missingTimestamps_excludedFromAvgResolutionHours() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of(
                request("APPROVED", null, null)));

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(30);

        assertEquals(1, summary.getTotalResolved());
        assertEquals(0.0, summary.getAvgResolutionHours());
    }

    @Test
    void getSummary_periodDaysReflectedInSummary() {
        when(requestRepository.findResolvedBetween(any(), any())).thenReturn(List.of());

        RequestAnalyticsSummary summary = requestAnalyticsService.getSummary(60);

        assertEquals(60, summary.getPeriodDays());
    }
}