package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.DashboardStats;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private DashboardService dashboardService;

    private Branch capeTown;
    private Branch testBranch;
    private Employee sam;
    private User hrUser;
    private User adminUser;
    private User mgrUser;
    private User empUser;

    @BeforeEach
    void setUp() {
        capeTown = new Branch();
        capeTown.setId(1L);
        capeTown.setName("Cape Town");

        testBranch = new Branch();
        testBranch.setId(2L);
        testBranch.setName("Test Branch");

        sam = new Employee();
        sam.setId(3L);
        sam.setBranch(capeTown);

        hrUser = new User();
        hrUser.setRole("HR");

        adminUser = new User();
        adminUser.setRole("ADMIN");

        mgrUser = new User();
        mgrUser.setRole("MANAGER");
        mgrUser.setEmployee(sam);

        empUser = new User();
        empUser.setRole("EMPLOYEE");
    }

    @Test
    void getStats_hr_returnsFullCompanyWideStats() {
        Employee emma = new Employee();
        emma.setBranch(capeTown);

        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(employeeRepository.findAll()).thenReturn(List.of(sam, emma));
        when(branchRepository.findAll()).thenReturn(List.of(capeTown, testBranch));
        when(leaveRequestRepository.findAll()).thenReturn(List.of());
        when(attendanceRepository.findAll()).thenReturn(List.of());

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getTotalEmployees()).isEqualTo(2);
        assertThat(stats.getEmployeesPerBranch()).containsEntry("Cape Town", 2L);
        assertThat(stats.getEmployeesPerBranch()).containsEntry("Test Branch", 0L);
    }

    @Test
    void getStats_admin_returnsFullCompanyWideStats() {
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(employeeRepository.findAll()).thenReturn(List.of(sam));
        when(branchRepository.findAll()).thenReturn(List.of(capeTown));
        when(leaveRequestRepository.findAll()).thenReturn(List.of());
        when(attendanceRepository.findAll()).thenReturn(List.of());

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getTotalEmployees()).isEqualTo(1);
    }

    @Test
    void getStats_manager_returnsBranchScopedStats() {
        LeaveRequest pendingLeave = new LeaveRequest();
        pendingLeave.setStatus("PENDING");

        when(currentUserService.getCurrentUser()).thenReturn(mgrUser);
        when(employeeRepository.findByBranchId(1L)).thenReturn(List.of(sam));
        when(leaveRequestRepository.findByStatusAndEmployeeBranchId("PENDING", 1L))
                .thenReturn(List.of(pendingLeave));
        when(leaveRequestRepository.findByStatusAndEmployeeBranchId("APPROVED", 1L))
                .thenReturn(List.of());
        when(leaveRequestRepository.findByStatusAndEmployeeBranchId("REJECTED", 1L))
                .thenReturn(List.of());
        when(attendanceRepository.findByEmployeeBranchId(1L)).thenReturn(List.of());

        DashboardStats stats = dashboardService.getStats();

        assertThat(stats.getTotalEmployees()).isEqualTo(1);
        assertThat(stats.getEmployeesPerBranch()).containsOnly(java.util.Map.entry("Cape Town", 1L));
        assertThat(stats.getPendingLeaveRequests()).isEqualTo(1);
    }

    @Test
    void getStats_managerWithNoBranch_throwsAccessDenied() {
        Employee noBranchEmployee = new Employee();
        noBranchEmployee.setId(9L);
        User mgrNoBranch = new User();
        mgrNoBranch.setRole("MANAGER");
        mgrNoBranch.setEmployee(noBranchEmployee);

        when(currentUserService.getCurrentUser()).thenReturn(mgrNoBranch);

        assertThatThrownBy(() -> dashboardService.getStats())
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getStats_employee_throwsAccessDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(empUser);

        assertThatThrownBy(() -> dashboardService.getStats())
                .isInstanceOf(AccessDeniedException.class);
    }
}