package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.RecruitmentAnalyticsSummary;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.repository.ApplicationRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecruitmentAnalyticsService {

    private final ApplicationRepository applicationRepository;

    public RecruitmentAnalyticsService(ApplicationRepository applicationRepository) {
        this.applicationRepository = applicationRepository;
    }

    public RecruitmentAnalyticsSummary getSummary(int periodDays) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(periodDays);

        List<Application> all = applicationRepository.findBySubmittedAtBetween(start, end);

        long totalApplications = all.size();

        List<Application> reviewed = all.stream()
                .filter(a -> a.getMeetsRequirements() != null)
                .toList();
        long reviewedCount = reviewed.size();
        long meetsRequirementsCount = reviewed.stream().filter(Application::getMeetsRequirements).count();
        double requirementsPassRate = reviewedCount > 0 ? (double) meetsRequirementsCount / reviewedCount : 0.0;

        List<Application> decided = all.stream()
                .filter(a -> a.getOutcome() != null)
                .toList();
        long decidedCount = decided.size();
        long acceptedCount = decided.stream().filter(a -> "ACCEPTED".equals(a.getOutcome())).count();
        double acceptanceRate = decidedCount > 0 ? (double) acceptedCount / decidedCount : 0.0;

        double avgDecisionHours = decided.stream()
                .filter(a -> a.getSubmittedAt() != null && a.getDecidedAt() != null)
                .mapToLong(a -> Duration.between(a.getSubmittedAt(), a.getDecidedAt()).toMinutes())
                .average()
                .orElse(0.0) / 60.0;

        RecruitmentAnalyticsSummary summary = new RecruitmentAnalyticsSummary();
        summary.setPeriodDays(periodDays);
        summary.setTotalApplications(totalApplications);
        summary.setReviewedCount(reviewedCount);
        summary.setMeetsRequirementsCount(meetsRequirementsCount);
        summary.setRequirementsPassRate(round(requirementsPassRate));
        summary.setDecidedCount(decidedCount);
        summary.setAcceptedCount(acceptedCount);
        summary.setAcceptanceRate(round(acceptanceRate));
        summary.setAvgDecisionHours(round(avgDecisionHours));

        return summary;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}