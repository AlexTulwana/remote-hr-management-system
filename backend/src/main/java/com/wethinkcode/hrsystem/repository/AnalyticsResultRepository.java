package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.AnalyticsResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AnalyticsResultRepository extends JpaRepository<AnalyticsResult, Long> {

    Optional<AnalyticsResult> findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
            String metricName, Long branchId);

    List<AnalyticsResult> findByMetricNameAndBranchIdAndMetricDateBetweenOrderByMetricDateAsc(
            String metricName, Long branchId, LocalDate start, LocalDate end);
}