package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.HeadcountSnapshot;
import com.wethinkcode.hrsystem.model.SalarySnapshot;
import com.wethinkcode.hrsystem.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HrDashboardService {

    private static final Long COMPANY_WIDE_BRANCH_ID = 0L;

    private final EmployeeRepository employeeRepository;
    private final HeadcountSnapshotRepository headcountSnapshotRepository;
    private final SalarySnapshotRepository salarySnapshotRepository;
    private final ApplicationRepository applicationRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRequestRepository employeeRequestRepository;
    private final DisciplinaryCaseRepository disciplinaryCaseRepository;
    private final AnalyticsResultRepository analyticsResultRepository;

    public HrDashboardService(EmployeeRepository employeeRepository,
                              HeadcountSnapshotRepository headcountSnapshotRepository,
                              SalarySnapshotRepository salarySnapshotRepository,
                              ApplicationRepository applicationRepository,
                              LeaveRequestRepository leaveRequestRepository,
                              EmployeeRequestRepository employeeRequestRepository,
                              DisciplinaryCaseRepository disciplinaryCaseRepository,
                              AnalyticsResultRepository analyticsResultRepository) {
        this.employeeRepository = employeeRepository;
        this.headcountSnapshotRepository = headcountSnapshotRepository;
        this.salarySnapshotRepository = salarySnapshotRepository;
        this.applicationRepository = applicationRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRequestRepository = employeeRequestRepository;
        this.disciplinaryCaseRepository = disciplinaryCaseRepository;
        this.analyticsResultRepository = analyticsResultRepository;
    }

    // 1. Headcount (company-wide)
    public ManagerHeadcountSummary getHeadcount() {
        List<com.wethinkcode.hrsystem.model.Employee> employees = employeeRepository.findAll();
        long total = employees.size();
        List<com.wethinkcode.hrsystem.model.Employee> active = employees.stream()
                .filter(e -> "ACTIVE".equals(e.getEmploymentStatus()))
                .toList();
        Map<String, Long> byDepartment = active.stream()
                .collect(Collectors.groupingBy(com.wethinkcode.hrsystem.model.Employee::getDepartment, Collectors.counting()));
        return new ManagerHeadcountSummary(total, active.size(), byDepartment);
    }

    // 2. Headcount trend
    public List<HeadcountTrendPoint> getHeadcountTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<HeadcountSnapshot> snapshots = headcountSnapshotRepository.findBySnapshotDateBetween(start, end);

        Map<LocalDate, Long> totalByDate = snapshots.stream()
                .filter(s -> "ACTIVE".equals(s.getEmploymentStatus()))
                .collect(Collectors.groupingBy(HeadcountSnapshot::getSnapshotDate,
                        Collectors.summingLong(HeadcountSnapshot::getHeadcount)));

        return totalByDate.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new HeadcountTrendPoint(e.getKey(), e.getValue()))
                .toList();
    }

    // 3. Recruitment funnel
    public RecruitmentFunnelSummary getRecruitmentFunnel() {
        List<Application> all = applicationRepository.findAll();
        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(Application::getStatus, Collectors.counting()));
        return new RecruitmentFunnelSummary(byStatus, all.size());
    }

    // 4. Pending items (all types, company-wide)
    public HrPendingItemsSummary getPendingItems() {
        List<LeaveRequestSummary> pendingLeave = leaveRequestRepository.findAll().stream()
                .filter(l -> "PENDING".equals(l.getStatus()))
                .map(LeaveRequestSummary::from)
                .toList();

        List<EmployeeRequestSummary> pendingRequests = employeeRequestRepository.findByStatus("PENDING").stream()
                .map(EmployeeRequestSummary::from)
                .toList();

        List<DisciplinaryCaseSummary> openCases = disciplinaryCaseRepository.findByClosedFalse().stream()
                .map(DisciplinaryCaseSummary::from)
                .toList();

        long pendingApplications = applicationRepository.findAll().stream()
                .filter(a -> a.getOutcome() == null)
                .count();

        long total = pendingLeave.size() + pendingRequests.size() + openCases.size() + pendingApplications;

        return new HrPendingItemsSummary(pendingLeave, pendingRequests, openCases, pendingApplications, total);
    }

    // 5. Turnover (latest company-wide value)
    public TurnoverSummary getTurnover() {
        return analyticsResultRepository
                .findTopByMetricNameAndBranchIdOrderByMetricDateDesc("turnover_rate_30d", COMPANY_WIDE_BRANCH_ID)
                .map(r -> new TurnoverSummary(r.getMetricDate(), r.getMetricValue()))
                .orElse(new TurnoverSummary(null, 0.0));
    }

    // 6. Salary summary (company-wide)
    public HrSalarySummary getSalarySummary() {
        List<com.wethinkcode.hrsystem.model.Employee> active = employeeRepository.findAll().stream()
                .filter(e -> "ACTIVE".equals(e.getEmploymentStatus()) && e.getSalary() != null)
                .toList();

        double total = active.stream().mapToDouble(com.wethinkcode.hrsystem.model.Employee::getSalary).sum();
        double average = active.isEmpty() ? 0.0 : total / active.size();

        Map<String, Double> byDepartment = active.stream()
                .collect(Collectors.groupingBy(com.wethinkcode.hrsystem.model.Employee::getDepartment,
                        Collectors.summingDouble(com.wethinkcode.hrsystem.model.Employee::getSalary)));

        return new HrSalarySummary(total, average, active.size(), byDepartment);
    }

    // 7. Salary trend
    public List<SalaryTrendPoint> getSalaryTrend(int days) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(days);

        List<SalarySnapshot> snapshots = salarySnapshotRepository.findBySnapshotDateBetween(start, end);

        Map<LocalDate, List<SalarySnapshot>> byDate = snapshots.stream()
                .collect(Collectors.groupingBy(SalarySnapshot::getSnapshotDate));

        List<SalaryTrendPoint> points = new ArrayList<>();
        for (Map.Entry<LocalDate, List<SalarySnapshot>> entry : byDate.entrySet()) {
            double total = entry.getValue().stream().mapToDouble(SalarySnapshot::getTotalSalary).sum();
            int employeeCount = entry.getValue().stream().mapToInt(SalarySnapshot::getEmployeeCount).sum();
            double average = employeeCount > 0 ? total / employeeCount : 0.0;
            points.add(new SalaryTrendPoint(entry.getKey(), total, average));
        }
        points.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        return points;
    }
}