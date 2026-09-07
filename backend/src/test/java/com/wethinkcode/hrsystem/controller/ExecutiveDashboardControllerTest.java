package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.service.ExecutiveDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExecutiveDashboardController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class ExecutiveDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private ExecutiveDashboardService executiveDashboardService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void getKpis_asAdmin_returns200() throws Exception {
        when(executiveDashboardService.getKpis()).thenReturn(
                new ExecutiveKpiSummary(10L, 8L, 0.05, 5L, 14L, 4L, 500000.0));

        mockMvc.perform(get("/api/dashboard/executive/kpis"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getKpis_asHr_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/executive/kpis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getKpis_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/executive/kpis"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getKpis_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/executive/kpis"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getBranchComparison_asAdmin_returns200() throws Exception {
        when(executiveDashboardService.getBranchComparison()).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/executive/branch-comparison"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getTurnoverTrend_asAdmin_returns200() throws Exception {
        when(executiveDashboardService.getTurnoverTrend(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/executive/turnover-trend"))
                .andExpect(status().isOk());
    }
}