package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.RecruitmentAnalyticsSummary;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import org.junit.jupiter.api.BeforeEach;
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
class RecruitmentAnalyticsServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @InjectMocks
    private RecruitmentAnalyticsService recruitmentAnalyticsService;

    private Application application(Boolean meetsRequirements, String outcome,
                                    LocalDateTime submittedAt, LocalDateTime decidedAt) {
        Application app = new Application();
        app.setMeetsRequirements(meetsRequirements);
        app.setOutcome(outcome);
        app.setSubmittedAt(submittedAt);
        app.setDecidedAt(decidedAt);
        return app;
    }

    @Test
    void getSummary_noApplications_returnsZeroedSummary() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of());

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(30, summary.getPeriodDays());
        assertEquals(0, summary.getTotalApplications());
        assertEquals(0, summary.getReviewedCount());
        assertEquals(0.0, summary.getRequirementsPassRate());
        assertEquals(0, summary.getDecidedCount());
        assertEquals(0.0, summary.getAcceptanceRate());
        assertEquals(0.0, summary.getAvgDecisionHours());
    }

    @Test
    void getSummary_totalApplications_countsAll() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of(
                application(null, null, null, null),
                application(null, null, null, null),
                application(null, null, null, null)));

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(3, summary.getTotalApplications());
    }

    @Test
    void getSummary_requirementsPassRate_computedFromReviewedOnly() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of(
                application(true, null, null, null),
                application(true, null, null, null),
                application(false, null, null, null),
                application(null, null, null, null))); // not reviewed, excluded

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(3, summary.getReviewedCount());
        assertEquals(2, summary.getMeetsRequirementsCount());
        assertEquals(0.6667, summary.getRequirementsPassRate());
    }

    @Test
    void getSummary_acceptanceRate_computedFromDecidedOnly() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of(
                application(null, "ACCEPTED", null, null),
                application(null, "REJECTED", null, null),
                application(null, "REJECTED", null, null),
                application(null, null, null, null))); // not decided, excluded

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(3, summary.getDecidedCount());
        assertEquals(1, summary.getAcceptedCount());
        assertEquals(0.3333, summary.getAcceptanceRate());
    }

    @Test
    void getSummary_avgDecisionHours_computedFromDecidedWithBothTimestamps() {
        LocalDateTime submitted1 = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime decided1 = LocalDateTime.of(2026, 1, 2, 9, 0); // 24 hours

        LocalDateTime submitted2 = LocalDateTime.of(2026, 1, 1, 9, 0);
        LocalDateTime decided2 = LocalDateTime.of(2026, 1, 1, 21, 0); // 12 hours

        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of(
                application(null, "ACCEPTED", submitted1, decided1),
                application(null, "REJECTED", submitted2, decided2)));

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(18.0, summary.getAvgDecisionHours()); // (24 + 12) / 2
    }

    @Test
    void getSummary_decidedWithoutTimestamps_excludedFromAvgDecisionHours() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of(
                application(null, "ACCEPTED", null, null))); // decided but no timestamps

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(30);

        assertEquals(1, summary.getDecidedCount());
        assertEquals(0.0, summary.getAvgDecisionHours());
    }

    @Test
    void getSummary_periodDaysReflectedInSummary() {
        when(applicationRepository.findBySubmittedAtBetween(any(), any())).thenReturn(List.of());

        RecruitmentAnalyticsSummary summary = recruitmentAnalyticsService.getSummary(90);

        assertEquals(90, summary.getPeriodDays());
    }
}