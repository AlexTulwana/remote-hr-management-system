package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.ManagerHeadcountSummary;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.ManagerDashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ManagerDashboardController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class ManagerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeRequestRepository employeeRequestRepository;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private ManagerDashboardService managerDashboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private Branch branch;
    private Employee employee;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);

        employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Sam Manager");
        employee.setBranch(branch);
    }

    private void mockCurrentManager() {
        User manager = new User();
        manager.setEmployee(employee);
        when(currentUserService.getCurrentUser()).thenReturn(manager);
    }

    // ---- getPendingRequests() ----

    @Test
    void getPendingRequests_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/pending-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPendingRequests_managerRole_isAllowed() throws Exception {
        mockCurrentManager();
        EmployeeRequest request = new EmployeeRequest();
        request.setEmployee(employee);
        when(employeeRequestRepository.findByStatusAndEmployeeBranchId("PENDING", 1L))
                .thenReturn(List.of(request));

        mockMvc.perform(get("/api/dashboard/manager/pending-requests"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getPendingRequests_hrRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/pending-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getPendingRequests_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/pending-requests"))
                .andExpect(status().isForbidden());
    }

    // ---- getHeadcount() ----

    @Test
    void getHeadcount_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/headcount"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getHeadcount_managerRole_isAllowed() throws Exception {
        mockCurrentManager();
        when(managerDashboardService.getHeadcount(1L)).thenReturn(new ManagerHeadcountSummary(3, 2, java.util.Map.of("IT", 2L)));

        mockMvc.perform(get("/api/dashboard/manager/headcount"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHeadcount_adminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/headcount"))
                .andExpect(status().isForbidden());
    }

    // ---- getDisciplinaryCases() ----

    @Test
    void getDisciplinaryCases_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/disciplinary-cases"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getDisciplinaryCases_managerRole_isAllowed() throws Exception {
        mockCurrentManager();
        when(managerDashboardService.getOpenDisciplinaryCases(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/manager/disciplinary-cases"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getDisciplinaryCases_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/disciplinary-cases"))
                .andExpect(status().isForbidden());
    }

    // ---- getPendingLeave() ----

    @Test
    void getPendingLeave_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/leave"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getPendingLeave_managerRole_isAllowed() throws Exception {
        mockCurrentManager();
        when(managerDashboardService.getPendingLeave(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/manager/leave"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getPendingLeave_hrRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/leave"))
                .andExpect(status().isForbidden());
    }

    // ---- getUpcomingSchedule() ----

    @Test
    void getUpcomingSchedule_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/schedule"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getUpcomingSchedule_managerRole_isAllowed() throws Exception {
        mockCurrentManager();
        when(managerDashboardService.getUpcomingSchedule(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/manager/schedule"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getUpcomingSchedule_adminRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/manager/schedule"))
                .andExpect(status().isForbidden());
    }
}
