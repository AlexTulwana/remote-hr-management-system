package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrDashboardServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private HeadcountSnapshotRepository headcountSnapshotRepository;
    @Mock private SalarySnapshotRepository salarySnapshotRepository;
    @Mock private ApplicationRepository applicationRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private EmployeeRequestRepository employeeRequestRepository;
    @Mock private DisciplinaryCaseRepository disciplinaryCaseRepository;
    @Mock private AnalyticsResultRepository analyticsResultRepository;

    @InjectMocks
    private HrDashboardService hrDashboardService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");
        employee.setDepartment("IT");
    }

    // ---------- getHeadcount() ----------

    @Test
    void getHeadcount_countsTotalAndActiveByDepartment() {
        Employee active1 = new Employee();
        active1.setEmploymentStatus("ACTIVE");
        active1.setDepartment("IT");

        Employee resigned = new Employee();
        resigned.setEmploymentStatus("RESIGNED");
        resigned.setDepartment("HR");

        when(employeeRepository.findAll()).thenReturn(List.of(active1, resigned));

        ManagerHeadcountSummary summary = hrDashboardService.getHeadcount();

        assertEquals(2, summary.getTotalEmployees());
        assertEquals(1, summary.getActiveEmployees());
        assertEquals(1, summary.getActiveByDepartment().get("IT"));
    }

    // ---------- getHeadcountTrend() ----------

    @Test
    void getHeadcountTrend_sumsActiveHeadcountByDate_sortedAscending() {
        LocalDate date1 = LocalDate.of(2026, 1, 1);
        LocalDate date2 = LocalDate.of(2026, 1, 2);

        HeadcountSnapshot s1 = new HeadcountSnapshot();
        s1.setSnapshotDate(date2);
        s1.setEmploymentStatus("ACTIVE");
        s1.setHeadcount(5);

        HeadcountSnapshot s2 = new HeadcountSnapshot();
        s2.setSnapshotDate(date1);
        s2.setEmploymentStatus("ACTIVE");
        s2.setHeadcount(3);

        HeadcountSnapshot s3 = new HeadcountSnapshot();
        s3.setSnapshotDate(date1);
        s3.setEmploymentStatus("RESIGNED");
        s3.setHeadcount(1); // excluded, not ACTIVE

        when(headcountSnapshotRepository.findBySnapshotDateBetween(any(), any()))
                .thenReturn(List.of(s1, s2, s3));

        List<HeadcountTrendPoint> result = hrDashboardService.getHeadcountTrend(30);

        assertEquals(2, result.size());
        assertEquals(date1, result.get(0).getDate());
        assertEquals(3, result.get(0).getTotalActive());
        assertEquals(date2, result.get(1).getDate());
        assertEquals(5, result.get(1).getTotalActive());
    }

    @Test
    void getHeadcountTrend_multipleActiveSnapshotsSameDate_summed() {
        LocalDate date = LocalDate.of(2026, 1, 1);

        HeadcountSnapshot s1 = new HeadcountSnapshot();
        s1.setSnapshotDate(date);
        s1.setEmploymentStatus("ACTIVE");
        s1.setHeadcount(3);

        HeadcountSnapshot s2 = new HeadcountSnapshot();
        s2.setSnapshotDate(date);
        s2.setEmploymentStatus("ACTIVE");
        s2.setHeadcount(4);

        when(headcountSnapshotRepository.findBySnapshotDateBetween(any(), any()))
                .thenReturn(List.of(s1, s2));

        List<HeadcountTrendPoint> result = hrDashboardService.getHeadcountTrend(30);

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getTotalActive());
    }

    // ---------- getRecruitmentFunnel() ----------

    @Test
    void getRecruitmentFunnel_countsByStatus() {
        Application a1 = new Application();
        a1.setStatus("SUBMITTED");
        Application a2 = new Application();
        a2.setStatus("SUBMITTED");
        Application a3 = new Application();
        a3.setStatus("HIRED");

        when(applicationRepository.findAll()).thenReturn(List.of(a1, a2, a3));

        RecruitmentFunnelSummary summary = hrDashboardService.getRecruitmentFunnel();

        assertEquals(3, summary.getTotalApplications());
        assertEquals(2, summary.getCountByStatus().get("SUBMITTED"));
        assertEquals(1, summary.getCountByStatus().get("HIRED"));
    }

    // ---------- getPendingItems() ----------

    @Test
    void getPendingItems_aggregatesAllTypesAndComputesTotal() {
        LeaveRequest pendingLeave = new LeaveRequest();
        pendingLeave.setStatus("PENDING");
        pendingLeave.setEmployee(employee);
        LeaveRequest approvedLeave = new LeaveRequest();
        approvedLeave.setStatus("APPROVED");
        approvedLeave.setEmployee(employee);

        when(leaveRequestRepository.findAll()).thenReturn(List.of(pendingLeave, approvedLeave));

        com.wethinkcode.hrsystem.model.EmployeeRequest pendingRequest = new com.wethinkcode.hrsystem.model.EmployeeRequest();
        pendingRequest.setStatus("PENDING");
        pendingRequest.setEmployee(employee);
        pendingRequest.setRequestType(RequestType.valueOf(RequestType.values()[0].name()));
        when(employeeRequestRepository.findByStatus("PENDING")).thenReturn(List.of(pendingRequest));

        DisciplinaryCase openCase = new DisciplinaryCase();
        openCase.setEmployee(employee);
        openCase.setCurrentStage(DisciplinaryStage.VERBAL_WARNING);
        User opener = new User();
        opener.setUsername("hrtest2");
        openCase.setOpenedBy(opener);
        when(disciplinaryCaseRepository.findByClosedFalse()).thenReturn(List.of(openCase));

        Application pendingApp = new Application();
        pendingApp.setOutcome(null);
        Application decidedApp = new Application();
        decidedApp.setOutcome("ACCEPTED");
        when(applicationRepository.findAll()).thenReturn(List.of(pendingApp, decidedApp));

        HrPendingItemsSummary summary = hrDashboardService.getPendingItems();

        assertEquals(1, summary.getPendingLeave().size());
        assertEquals(1, summary.getPendingRequests().size());
        assertEquals(1, summary.getOpenDisciplinaryCases().size());
        assertEquals(1, summary.getPendingApplications());
        assertEquals(4, summary.getTotalPendingCount());
    }

    @Test
    void getPendingItems_allEmpty_totalIsZero() {
        when(leaveRequestRepository.findAll()).thenReturn(List.of());
        when(employeeRequestRepository.findByStatus("PENDING")).thenReturn(List.of());
        when(disciplinaryCaseRepository.findByClosedFalse()).thenReturn(List.of());
        when(applicationRepository.findAll()).thenReturn(List.of());

        HrPendingItemsSummary summary = hrDashboardService.getPendingItems();

        assertEquals(0, summary.getTotalPendingCount());
    }

    // ---------- getTurnover() ----------

    @Test
    void getTurnover_found_returnsLatestValue() {
        AnalyticsResult result = new AnalyticsResult();
        result.setMetricDate(LocalDate.of(2026, 1, 15));
        result.setMetricValue(0.045);

        when(analyticsResultRepository.findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
                eq("turnover_rate_30d"), eq(0L))).thenReturn(Optional.of(result));

        TurnoverSummary summary = hrDashboardService.getTurnover();

        assertEquals(LocalDate.of(2026, 1, 15), summary.getAsOfDate());
        assertEquals(0.045, summary.getTurnoverRate30d());
    }

    @Test
    void getTurnover_notFound_returnsZeroedDefault() {
        when(analyticsResultRepository.findTopByMetricNameAndBranchIdOrderByMetricDateDesc(
                eq("turnover_rate_30d"), eq(0L))).thenReturn(Optional.empty());

        TurnoverSummary summary = hrDashboardService.getTurnover();

        assertNull(summary.getAsOfDate());
        assertEquals(0.0, summary.getTurnoverRate30d());
    }

    // ---------- getSalarySummary() ----------

    @Test
    void getSalarySummary_computesTotalsAverageAndByDepartment() {
        Employee e1 = new Employee();
        e1.setEmploymentStatus("ACTIVE");
        e1.setDepartment("IT");
        e1.setSalary(30000.0);

        Employee e2 = new Employee();
        e2.setEmploymentStatus("ACTIVE");
        e2.setDepartment("IT");
        e2.setSalary(50000.0);

        Employee e3 = new Employee();
        e3.setEmploymentStatus("RESIGNED");
        e3.setDepartment("IT");
        e3.setSalary(40000.0); // excluded, not ACTIVE

        Employee e4 = new Employee();
        e4.setEmploymentStatus("ACTIVE");
        e4.setDepartment("HR");
        e4.setSalary(null); // excluded, null salary

        when(employeeRepository.findAll()).thenReturn(List.of(e1, e2, e3, e4));

        HrSalarySummary summary = hrDashboardService.getSalarySummary();

        assertEquals(80000.0, summary.getTotalSalary());
        assertEquals(40000.0, summary.getAverageSalary());
        assertEquals(2, summary.getEmployeeCount());
        assertEquals(80000.0, summary.getTotalSalaryByDepartment().get("IT"));
    }

    @Test
    void getSalarySummary_noActiveEmployees_returnsZeroedAverage() {
        when(employeeRepository.findAll()).thenReturn(List.of());

        HrSalarySummary summary = hrDashboardService.getSalarySummary();

        assertEquals(0.0, summary.getTotalSalary());
        assertEquals(0.0, summary.getAverageSalary());
        assertEquals(0, summary.getEmployeeCount());
    }

    // ---------- getSalaryTrend() ----------

    @Test
    void getSalaryTrend_aggregatesByDateAcrossDepartments() {
        LocalDate date = LocalDate.of(2026, 1, 1);

        SalarySnapshot s1 = new SalarySnapshot();
        s1.setSnapshotDate(date);
        s1.setTotalSalary(50000.0);
        s1.setEmployeeCount(2);

        SalarySnapshot s2 = new SalarySnapshot();
        s2.setSnapshotDate(date);
        s2.setTotalSalary(30000.0);
        s2.setEmployeeCount(1);

        when(salarySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of(s1, s2));

        List<SalaryTrendPoint> result = hrDashboardService.getSalaryTrend(30);

        assertEquals(1, result.size());
        assertEquals(80000.0, result.get(0).getTotalSalary());
        assertEquals(80000.0 / 3, result.get(0).getAverageSalary(), 0.0001);
    }

    @Test
    void getSalaryTrend_sortedByDateAscending() {
        LocalDate date1 = LocalDate.of(2026, 1, 1);
        LocalDate date2 = LocalDate.of(2026, 1, 5);

        SalarySnapshot s1 = new SalarySnapshot();
        s1.setSnapshotDate(date2);
        s1.setTotalSalary(10000.0);
        s1.setEmployeeCount(1);

        SalarySnapshot s2 = new SalarySnapshot();
        s2.setSnapshotDate(date1);
        s2.setTotalSalary(20000.0);
        s2.setEmployeeCount(1);

        when(salarySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of(s1, s2));

        List<SalaryTrendPoint> result = hrDashboardService.getSalaryTrend(30);

        assertEquals(date1, result.get(0).getDate());
        assertEquals(date2, result.get(1).getDate());
    }

    @Test
    void getSalaryTrend_noSnapshots_returnsEmptyList() {
        when(salarySnapshotRepository.findBySnapshotDateBetween(any(), any())).thenReturn(List.of());

        assertTrue(hrDashboardService.getSalaryTrend(30).isEmpty());
    }
}