package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.model.AnalyticsResult;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.repository.AnalyticsResultRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExecutiveDashboardService {

    private static final Long COMPANY_WIDE_BRANCH_ID = 0L;

    private final HrDashboardService hrDashboardService;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final AnalyticsResultRepository analyticsResultRepository;

    public ExecutiveDashboardService(HrDashboardService hrDashboardService,
                                     EmployeeRepository employeeRepository,
                                     BranchRepository branchRepository,
                                     AnalyticsResultRepository analyticsResultRepository) {
        this.hrDashboardService = hrDashboardService;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.analyticsResultRepository = analyticsResultRepository;
    }

    public ExecutiveKpiSummary getKpis() {
        ManagerHeadcountSummary headcount = hrDashboardService.getHeadcount();
        TurnoverSummary turnover = hrDashboardService.getTurnover();
        HrPendingItemsSummary pending = hrDashboardService.getPendingItems();
        RecruitmentFunnelSummary funnel = hrDashboardService.getRecruitmentFunnel();
        HrSalarySummary salary = hrDashboardService.getSalarySummary();

        long hired = funnel.getCountByStatus().getOrDefault("HIRED", 0L);

        return new ExecutiveKpiSummary(
                headcount.getTotalEmployees(),
                headcount.getActiveEmployees(),
                turnover.getTurnoverRate30d(),
                pending.getTotalPendingCount(),
                funnel.getTotalApplications(),
                hired,
                salary.getTotalSalary()
        );
    }

    public List<BranchComparisonEntry> getBranchComparison() {
        List<Branch> branches = branchRepository.findAll();

        Map<Long, Long> activeHeadcountByBranch = new HashMap<>();
        employeeRepository.aggregateByBranchDeptStatus().stream()
                .filter(agg -> "ACTIVE".equals(agg.getEmploymentStatus()))
                .forEach(agg -> activeHeadcountByBranch.merge(agg.getBranchId(), agg.getHeadcount(), Long::sum));

        Map<Long, Double> totalSalaryByBranch = new HashMap<>();
        Map<Long, Long> employeeCountByBranch = new HashMap<>();
        employeeRepository.aggregateSalaryByBranchDept().forEach(agg -> {
            totalSalaryByBranch.merge(agg.getBranchId(), agg.getTotalSalary().doubleValue(), Double::sum);
            employeeCountByBranch.merge(agg.getBranchId(), agg.getEmployeeCount(), Long::sum);
        });

        return branches.stream()
                .map(branch -> {
                    long active = activeHeadcountByBranch.getOrDefault(branch.getId(), 0L);
                    double totalSalary = totalSalaryByBranch.getOrDefault(branch.getId(), 0.0);
                    long employeeCount = employeeCountByBranch.getOrDefault(branch.getId(), 0L);
                    double avgSalary = employeeCount > 0 ? totalSalary / employeeCount : 0.0;
                    double turnover = analyticsResultRepository
                            .findTopByMetricNameAndBranchIdOrderByMetricDateDesc("turnover_rate_30d", branch.getId())
                            .map(AnalyticsResult::getMetricValue)
                            .orElse(0.0);
                    return new BranchComparisonEntry(branch.getId(), branch.getName(), active, totalSalary, avgSalary, turnover);
                })
                .toList();
    }

    public List<TurnoverTrendPoint> getTurnoverTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<AnalyticsResult> results = analyticsResultRepository
                .findByMetricNameAndBranchIdAndMetricDateBetweenOrderByMetricDateAsc(
                        "turnover_rate_30d", COMPANY_WIDE_BRANCH_ID, start, end);

        return results.stream()
                .map(r -> new TurnoverTrendPoint(r.getMetricDate(), r.getMetricValue()))
                .toList();
    }
}