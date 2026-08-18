package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.RequestAnalyticsSummary;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestAnalyticsService {

    private final EmployeeRequestRepository requestRepository;

    public RequestAnalyticsService(EmployeeRequestRepository requestRepository) {
        this.requestRepository = requestRepository;
    }

    public RequestAnalyticsSummary getSummary(int periodDays) {
        LocalDateTime end = LocalDateTime.now();
        LocalDateTime start = end.minusDays(periodDays);

        List<EmployeeRequest> resolved = requestRepository.findResolvedBetween(start, end);

        long total = resolved.size();
        long approved = resolved.stream().filter(r -> "APPROVED".equals(r.getStatus())).count();
        long rejected = resolved.stream().filter(r -> "REJECTED".equals(r.getStatus())).count();
        long escalated = resolved.stream().filter(r -> "ESCALATED".equals(r.getStatus())).count();

        double approvalRate = total > 0 ? (double) approved / total : 0.0;
        double escalationRate = total > 0 ? (double) escalated / total : 0.0;

        double avgResolutionHours = resolved.stream()
                .filter(r -> r.getSubmittedAt() != null && r.getResolvedAt() != null)
                .mapToLong(r -> Duration.between(r.getSubmittedAt(), r.getResolvedAt()).toMinutes())
                .average()
                .orElse(0.0) / 60.0;

        RequestAnalyticsSummary summary = new RequestAnalyticsSummary();
        summary.setPeriodDays(periodDays);
        summary.setTotalResolved(total);
        summary.setApproved(approved);
        summary.setRejected(rejected);
        summary.setEscalated(escalated);
        summary.setApprovalRate(round(approvalRate));
        summary.setEscalationRate(round(escalationRate));
        summary.setAvgResolutionHours(round(avgResolutionHours));

        return summary;
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }
}