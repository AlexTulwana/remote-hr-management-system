package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ManagerHeadcountSummary;
import com.wethinkcode.hrsystem.dto.ManagerScheduleItem;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerDashboardServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private DisciplinaryCaseRepository disciplinaryCaseRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private CalendarEventRepository calendarEventRepository;
    @Mock private HearingRepository hearingRepository;

    @InjectMocks
    private ManagerDashboardService managerDashboardService;

    private Branch branch;
    private Employee employee;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);
        branch.setName("Cape Town");

        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");
        employee.setBranch(branch);
    }

    // ---------- getHeadcount() ----------

    @Test
    void getHeadcount_countsTotalAndActiveByDepartment() {
        Employee active1 = new Employee();
        active1.setEmploymentStatus("ACTIVE");
        active1.setDepartment("IT");

        Employee active2 = new Employee();
        active2.setEmploymentStatus("ACTIVE");
        active2.setDepartment("IT");

        Employee resigned = new Employee();
        resigned.setEmploymentStatus("RESIGNED");
        resigned.setDepartment("HR");

        when(employeeRepository.findByBranchId(1L)).thenReturn(List.of(active1, active2, resigned));

        ManagerHeadcountSummary summary = managerDashboardService.getHeadcount(1L);

        assertEquals(3, summary.getTotalEmployees());
        assertEquals(2, summary.getActiveEmployees());
        assertEquals(2, summary.getActiveByDepartment().get("IT"));
        assertNull(summary.getActiveByDepartment().get("HR"));
    }

    @Test
    void getHeadcount_noEmployees_returnsZeroed() {
        when(employeeRepository.findByBranchId(1L)).thenReturn(List.of());

        ManagerHeadcountSummary summary = managerDashboardService.getHeadcount(1L);

        assertEquals(0, summary.getTotalEmployees());
        assertEquals(0, summary.getActiveEmployees());
        assertTrue(summary.getActiveByDepartment().isEmpty());
    }

    // ---------- getOpenDisciplinaryCases() ----------

    @Test
    void getOpenDisciplinaryCases_mapsToSummaries() {
        DisciplinaryCase c = new DisciplinaryCase();
        c.setId(5L);
        c.setReason("Late arrivals");
        c.setCurrentStage(DisciplinaryStage.VERBAL_WARNING);
        c.setEmployee(employee);
        c.setOpenedAt(LocalDateTime.now());

        User opener = new User();
        opener.setUsername("hrtest2");
        c.setOpenedBy(opener);

        when(disciplinaryCaseRepository.findByEmployeeBranchIdAndClosedFalse(1L)).thenReturn(List.of(c));

        var result = managerDashboardService.getOpenDisciplinaryCases(1L);

        assertEquals(1, result.size());
        assertEquals("Late arrivals", result.get(0).getReason());
        assertEquals("hrtest2", result.get(0).getOpenedByUsername());
        assertEquals("Emma Employee", result.get(0).getEmployee().getFullName());
    }

    @Test
    void getOpenDisciplinaryCases_empty_returnsEmptyList() {
        when(disciplinaryCaseRepository.findByEmployeeBranchIdAndClosedFalse(1L)).thenReturn(List.of());

        assertTrue(managerDashboardService.getOpenDisciplinaryCases(1L).isEmpty());
    }

    // ---------- getPendingLeave() ----------

    @Test
    void getPendingLeave_mapsToSummaries() {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(10L);
        leave.setLeaveType("ANNUAL");
        leave.setStartDate(LocalDate.of(2026, 2, 1));
        leave.setEndDate(LocalDate.of(2026, 2, 5));
        leave.setStatus("PENDING");
        leave.setEmployee(employee);

        when(leaveRequestRepository.findByStatusAndEmployeeBranchId("PENDING", 1L)).thenReturn(List.of(leave));

        var result = managerDashboardService.getPendingLeave(1L);

        assertEquals(1, result.size());
        assertEquals("ANNUAL", result.get(0).getLeaveType());
        assertEquals("Emma Employee", result.get(0).getEmployee().getFullName());
    }

    // ---------- getUpcomingSchedule() ----------

    @Test
    void getUpcomingSchedule_combinesCompanyWideBranchEventsAndHearings_sortedByDateTime() {
        CalendarEvent companyEvent = new CalendarEvent();
        companyEvent.setTitle("Company Meeting");
        companyEvent.setEventDate(LocalDate.now().plusDays(5));

        CalendarEvent branchEvent = new CalendarEvent();
        branchEvent.setTitle("Branch Meeting");
        branchEvent.setEventDate(LocalDate.now().plusDays(2));

        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setHearingDateTime(LocalDateTime.now().plusDays(1));
        hearing.setMeetingLink("https://meet.example.com");

        when(calendarEventRepository.findByBranchIsNullAndEventDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of(companyEvent));
        when(calendarEventRepository.findByBranchIdAndEventDateLessThanEqualAndEndDateGreaterThanEqual(eq(1L), any(), any()))
                .thenReturn(List.of(branchEvent));
        when(hearingRepository.findByEmployeeBranchIdAndHearingDateTimeBetweenAndStatus(eq(1L), any(), any(), eq("SCHEDULED")))
                .thenReturn(List.of(hearing));

        List<ManagerScheduleItem> result = managerDashboardService.getUpcomingSchedule(1L);

        assertEquals(3, result.size());
        // sorted ascending by dateTime: hearing (day+1), branchEvent (day+2), companyEvent (day+5)
        assertEquals("HEARING", result.get(0).getType());
        assertEquals("Branch Meeting", result.get(1).getTitle());
        assertEquals("Company Meeting", result.get(2).getTitle());
    }

    @Test
    void getUpcomingSchedule_hearingTitle_includesEmployeeName() {
        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setHearingDateTime(LocalDateTime.now().plusDays(1));

        when(calendarEventRepository.findByBranchIsNullAndEventDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of());
        when(calendarEventRepository.findByBranchIdAndEventDateLessThanEqualAndEndDateGreaterThanEqual(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(hearingRepository.findByEmployeeBranchIdAndHearingDateTimeBetweenAndStatus(eq(1L), any(), any(), eq("SCHEDULED")))
                .thenReturn(List.of(hearing));

        List<ManagerScheduleItem> result = managerDashboardService.getUpcomingSchedule(1L);

        assertEquals(1, result.size());
        assertEquals("Disciplinary Hearing: Emma Employee", result.get(0).getTitle());
    }

    @Test
    void getUpcomingSchedule_noItems_returnsEmptyList() {
        when(calendarEventRepository.findByBranchIsNullAndEventDateLessThanEqualAndEndDateGreaterThanEqual(any(), any()))
                .thenReturn(List.of());
        when(calendarEventRepository.findByBranchIdAndEventDateLessThanEqualAndEndDateGreaterThanEqual(eq(1L), any(), any()))
                .thenReturn(List.of());
        when(hearingRepository.findByEmployeeBranchIdAndHearingDateTimeBetweenAndStatus(eq(1L), any(), any(), eq("SCHEDULED")))
                .thenReturn(List.of());

        assertTrue(managerDashboardService.getUpcomingSchedule(1L).isEmpty());
    }
}