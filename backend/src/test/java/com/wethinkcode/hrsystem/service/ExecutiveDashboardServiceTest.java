package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.model.AnalyticsResult;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection;
import com.wethinkcode.hrsystem.repository.SalaryAggregateProjection;


import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class ExecutiveDashboardServiceTest {

    @Mock private HrDashboardService hrDashboardService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private AnalyticsResultRepository analyticsResultRepository;

    private ExecutiveDashboardService executiveDashboardService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        executiveDashboardService = new ExecutiveDashboardService(
                hrDashboardService, employeeRepository, branchRepository, analyticsResultRepository);
    }

    // ---------- getKpis() ----------

    @Test
    void getKpis_aggregatesFromHrDashboardService() {
        when(hrDashboardService.getHeadcount()).thenReturn(new ManagerHeadcountSummary(100L, 90L, Map.of()));
        when(hrDashboardService.getTurnover()).thenReturn(new TurnoverSummary(LocalDate.of(2026, 1, 1), 0.05));
        when(hrDashboardService.getPendingItems()).thenReturn(
                new HrPendingItemsSummary(List.of(), List.of(), List.of(), 3L, 5L));
        when(hrDashboardService.getRecruitmentFunnel()).thenReturn(
                new RecruitmentFunnelSummary(Map.of("HIRED", 4L, "SUBMITTED", 10L), 14L));
        when(hrDashboardService.getSalarySummary()).thenReturn(
                new HrSalarySummary(500000.0, 5555.0, 90L, Map.of()));

        ExecutiveKpiSummary kpis = executiveDashboardService.getKpis();

        assertEquals(100L, kpis.getTotalEmployees());
        assertEquals(90L, kpis.getActiveEmployees());
        assertEquals(0.05, kpis.getTurnoverRate30d());
        assertEquals(5L, kpis.getTotalPendingItems());
        assertEquals(14L, kpis.getTotalApplications());
        assertEquals(4L, kpis.getHiredCount());
        assertEquals(500000.0, kpis.getTotalSalarySpend());
    }

    @Test
    void getKpis_noHiredStatus_defaultsToZero() {
        when(hrDashboardService.getHeadcount()).thenReturn(new ManagerHeadcountSummary(10L, 10L, Map.of()));
        when(hrDashboardService.getTurnover()).thenReturn(new TurnoverSummary(null, 0.0));
        when(hrDashboardService.getPendingItems()).thenReturn(
                new HrPendingItemsSummary(List.of(), List.of(), List.of(), 0L, 0L));
        when(hrDashboardService.getRecruitmentFunnel()).thenReturn(
                new RecruitmentFunnelSummary(Map.of("SUBMITTED", 2L), 2L));
        when(hrDashboardService.getSalarySummary()).thenReturn(
                new HrSalarySummary(0.0, 0.0, 0L, Map.of()));

        ExecutiveKpiSummary kpis = executiveDashboardService.getKpis();

        assertEquals(0L, kpis.getHiredCount());
    }

    // ---------- getBranchComparison() ----------

    @Test
    void getBranchComparison_combinesHeadcountSalaryAndTurnoverPerBranch() {
        Branch branch1 = new Branch();
        branch1.setId(1L);
        branch1.setName("Cape Town");

        Branch branch2 = new Branch();
        branch2.setId(2L);
        branch2.setName("Johannesburg");

        when(branchRepository.findAll()).thenReturn(List.of(branch1, branch2));

        HeadcountAggregateProjection headcountAgg1 = mockHeadcountAgg(1L, "ACTIVE", 5L);
        HeadcountAggregateProjection headcountAgg2 = mockHeadcountAgg(1L, "ACTIVE", 3L);
        HeadcountAggregateProjection headcountAgg3 = mockHeadcountAgg(2L, "RESIGNED", 2L); // excluded, not ACTIVE
        when(employeeRepository.aggregateByBranchDeptStatus())
                .thenReturn(List.of(headcountAgg1, headcountAgg2, headcountAgg3));

        SalaryAggregateProjection salaryAgg1 = mockSalaryAgg(1L, 80000.0, 2L);
        SalaryAggregateProjection salaryAgg2 = mockSalaryAgg(2L, 40000.0, 1L);
        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of(salaryAgg1, salaryAgg2));

        AnalyticsResult turnoverResult = new AnalyticsResult();
        turnoverResult.setMetricValue(0.03);
        when(analyticsResultRepository.findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
                eq("turnover_rate_30d"), eq(1L))).thenReturn(Optional.of(turnoverResult));
        when(analyticsResultRepository.findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
                eq("turnover_rate_30d"), eq(2L))).thenReturn(Optional.empty());

        List<BranchComparisonEntry> result = executiveDashboardService.getBranchComparison();

        assertEquals(2, result.size());

        BranchComparisonEntry cptEntry = result.stream().filter(e -> e.getBranchId().equals(1L)).findFirst().get();
        assertEquals(8L, cptEntry.getActiveHeadcount()); // 5 + 3
        assertEquals(80000.0, cptEntry.getTotalSalary());
        assertEquals(40000.0, cptEntry.getAverageSalary()); // 80000 / 2
        assertEquals(0.03, cptEntry.getTurnoverRate30d());

        BranchComparisonEntry jhbEntry = result.stream().filter(e -> e.getBranchId().equals(2L)).findFirst().get();
        assertEquals(0L, jhbEntry.getActiveHeadcount()); // resigned excluded
        assertEquals(40000.0, jhbEntry.getTotalSalary());
        assertEquals(40000.0, jhbEntry.getAverageSalary()); // 40000 / 1
        assertEquals(0.0, jhbEntry.getTurnoverRate30d()); // not found, defaults to 0
    }

    @Test
    void getBranchComparison_branchWithNoData_returnsZeroedEntry() {
        Branch branch = new Branch();
        branch.setId(3L);
        branch.setName("Durban");

        when(branchRepository.findAll()).thenReturn(List.of(branch));
        when(employeeRepository.aggregateByBranchDeptStatus()).thenReturn(List.of());
        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of());
        when(analyticsResultRepository.findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
                eq("turnover_rate_30d"), eq(3L))).thenReturn(Optional.empty());

        List<BranchComparisonEntry> result = executiveDashboardService.getBranchComparison();

        assertEquals(1, result.size());
        assertEquals(0L, result.get(0).getActiveHeadcount());
        assertEquals(0.0, result.get(0).getTotalSalary());
        assertEquals(0.0, result.get(0).getAverageSalary());
        assertEquals(0.0, result.get(0).getTurnoverRate30d());
    }

    @Test
    void getBranchComparison_noBranches_returnsEmptyList() {
        when(branchRepository.findAll()).thenReturn(List.of());
        when(employeeRepository.aggregateByBranchDeptStatus()).thenReturn(List.of());
        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of());

        assertTrue(executiveDashboardService.getBranchComparison().isEmpty());
    }

    // ---------- getTurnoverTrend() ----------

    @Test
    void getTurnoverTrend_mapsResultsToPoints() {
        AnalyticsResult r1 = new AnalyticsResult();
        r1.setMetricDate(LocalDate.of(2026, 1, 1));
        r1.setMetricValue(0.02);

        AnalyticsResult r2 = new AnalyticsResult();
        r2.setMetricDate(LocalDate.of(2026, 1, 15));
        r2.setMetricValue(0.04);

        when(analyticsResultRepository.findByMetricNameAndBranchIdAndMetricDateBetweenOrderByMetricDateAsc(
                eq("turnover_rate_30d"), eq(0L), any(), any())).thenReturn(List.of(r1, r2));

        List<TurnoverTrendPoint> result = executiveDashboardService.getTurnoverTrend(30);

        assertEquals(2, result.size());
        assertEquals(LocalDate.of(2026, 1, 1), result.get(0).getDate());
        assertEquals(0.02, result.get(0).getTurnoverRate());
    }

    @Test
    void getTurnoverTrend_noResults_returnsEmptyList() {
        when(analyticsResultRepository.findByMetricNameAndBranchIdAndMetricDateBetweenOrderByMetricDateAsc(
                eq("turnover_rate_30d"), eq(0L), any(), any())).thenReturn(List.of());

        assertTrue(executiveDashboardService.getTurnoverTrend(30).isEmpty());
    }

    // ---------- helpers ----------

    private com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection mockHeadcountAgg(Long branchId, String status, Long headcount) {
        com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection agg = org.mockito.Mockito.mock(com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection.class);
        when(agg.getBranchId()).thenReturn(branchId);
        when(agg.getEmploymentStatus()).thenReturn(status);
        when(agg.getHeadcount()).thenReturn(headcount);
        return agg;
    }

    private com.wethinkcode.hrsystem.repository.SalaryAggregateProjection mockSalaryAgg(Long branchId, Double totalSalary, Long employeeCount) {
        com.wethinkcode.hrsystem.repository.SalaryAggregateProjection agg = org.mockito.Mockito.mock(com.wethinkcode.hrsystem.repository.SalaryAggregateProjection.class);
        when(agg.getBranchId()).thenReturn(branchId);
        when(agg.getTotalSalary()).thenReturn(totalSalary);
        when(agg.getEmployeeCount()).thenReturn(employeeCount);
        return agg;
    }
}