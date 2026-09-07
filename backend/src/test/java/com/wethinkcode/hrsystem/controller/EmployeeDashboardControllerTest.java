package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDashboardSummary;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeDashboardController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EmployeeDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeDashboardService employeeDashboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getSummary_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard/employee/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSummary_hrRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/dashboard/employee/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getSummary_employeeRole_isAllowed() throws Exception {
        when(employeeDashboardService.getSummary()).thenReturn(mock(EmployeeDashboardSummary.class));

        mockMvc.perform(get("/api/dashboard/employee/summary"))
                .andExpect(status().isOk());
    }
}