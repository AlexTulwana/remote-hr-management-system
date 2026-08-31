package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeDashboardSummary;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDashboardServiceTest {

    @Mock private CurrentUserService currentUserService;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRequestRepository employeeRequestRepository;
    @Mock private DisciplinaryCaseRepository disciplinaryCaseRepository;

    @InjectMocks
    private EmployeeDashboardService employeeDashboardService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        Branch branch = new Branch();
        branch.setName("Cape Town");

        employee = new Employee();
        employee.setId(1L);
        employee.setEmployeeNumber("EMP001");
        employee.setFullName("Emma Employee");
        employee.setPosition("Developer");
        employee.setDepartment("IT");
        employee.setBranch(branch);
        employee.setEmploymentStatus("ACTIVE");

        User user = new User();
        user.setEmployee(employee);
        when(currentUserService.getCurrentUser()).thenReturn(user);
    }

    @Test
    void getSummary_countsLeaveByStatus() {
        LeaveRequest pending = new LeaveRequest();
        pending.setStatus("PENDING");
        LeaveRequest approved1 = new LeaveRequest();
        approved1.setStatus("APPROVED");
        LeaveRequest approved2 = new LeaveRequest();
        approved2.setStatus("APPROVED");
        LeaveRequest rejected = new LeaveRequest();
        rejected.setStatus("REJECTED");

        when(leaveRequestRepository.findByEmployeeId(1L))
                .thenReturn(List.of(pending, approved1, approved2, rejected));
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(employeeRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(disciplinaryCaseRepository.findByEmployeeId(1L)).thenReturn(List.of());

        EmployeeDashboardSummary summary = employeeDashboardService.getSummary();

        assertEquals(1, summary.getPendingLeaveRequests());
        assertEquals(2, summary.getApprovedLeaveRequests());
        assertEquals(1, summary.getRejectedLeaveRequests());
        assertEquals("EMP001", summary.getEmployeeNumber());
        assertEquals("Cape Town", summary.getBranchName());
    }

    @Test
    void getSummary_attendanceThisMonth_countsOnlyCurrentMonthRecords() {
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);

        Attendance thisMonth = new Attendance();
        thisMonth.setDate(monthStart.plusDays(2));

        Attendance lastMonth = new Attendance();
        lastMonth.setDate(monthStart.minusDays(5));

        Attendance nullDate = new Attendance();
        nullDate.setDate(null);

        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of(thisMonth, lastMonth, nullDate));
        when(employeeRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(disciplinaryCaseRepository.findByEmployeeId(1L)).thenReturn(List.of());

        EmployeeDashboardSummary summary = employeeDashboardService.getSummary();

        assertEquals(1, summary.getAttendanceRecordsThisMonth());
    }

    @Test
    void getSummary_pendingRequestsAndOpenCases_countedCorrectly() {
        EmployeeRequest pendingReq = new EmployeeRequest();
        pendingReq.setStatus("PENDING");
        EmployeeRequest resolvedReq = new EmployeeRequest();
        resolvedReq.setStatus("APPROVED");

        DisciplinaryCase openCase = new DisciplinaryCase();
        openCase.setClosed(false);
        DisciplinaryCase closedCase = new DisciplinaryCase();
        closedCase.setClosed(true);

        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(employeeRequestRepository.findByEmployeeId(1L)).thenReturn(List.of(pendingReq, resolvedReq));
        when(disciplinaryCaseRepository.findByEmployeeId(1L)).thenReturn(List.of(openCase, closedCase));

        EmployeeDashboardSummary summary = employeeDashboardService.getSummary();

        assertEquals(1, summary.getPendingRequestsCount());
        assertEquals(1, summary.getOpenDisciplinaryCasesCount());
    }

    @Test
    void getSummary_noBranch_returnsNullBranchName() {
        employee.setBranch(null);

        when(leaveRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(attendanceRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(employeeRequestRepository.findByEmployeeId(1L)).thenReturn(List.of());
        when(disciplinaryCaseRepository.findByEmployeeId(1L)).thenReturn(List.of());

        EmployeeDashboardSummary summary = employeeDashboardService.getSummary();

        assertNull(summary.getBranchName());
    }
}