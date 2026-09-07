package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.dto.*;
import com.wethinkcode.hrsystem.service.HrDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HrDashboardController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class HrDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private HrDashboardService hrDashboardService;

    @Test
    @WithMockUser(roles = "HR")
    void getHeadcount_asHr_returns200() throws Exception {
        when(hrDashboardService.getHeadcount())
                .thenReturn(new ManagerHeadcountSummary(10L, 8L, Map.of()));

        mockMvc.perform(get("/api/dashboard/hr/headcount"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getHeadcount_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/hr/headcount"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getHeadcount_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/dashboard/hr/headcount"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getHeadcountTrend_asAdmin_returns200() throws Exception {
        when(hrDashboardService.getHeadcountTrend(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/hr/headcount-trend"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getRecruitmentFunnel_asHr_returns200() throws Exception {
        when(hrDashboardService.getRecruitmentFunnel())
                .thenReturn(new RecruitmentFunnelSummary(Map.of(), 0L));

        mockMvc.perform(get("/api/dashboard/hr/recruitment-funnel"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getPendingItems_asHr_returns200() throws Exception {
        when(hrDashboardService.getPendingItems())
                .thenReturn(new HrPendingItemsSummary(List.of(), List.of(), List.of(), 0L, 0L));

        mockMvc.perform(get("/api/dashboard/hr/pending-items"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getTurnover_asHr_returns200() throws Exception {
        when(hrDashboardService.getTurnover())
                .thenReturn(new TurnoverSummary(LocalDate.now(), 0.0));

        mockMvc.perform(get("/api/dashboard/hr/turnover"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSalarySummary_asHr_returns200() throws Exception {
        when(hrDashboardService.getSalarySummary())
                .thenReturn(new HrSalarySummary(0.0, 0.0, 0L, Map.of()));

        mockMvc.perform(get("/api/dashboard/hr/salary-summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSalaryTrend_asHr_returns200() throws Exception {
        when(hrDashboardService.getSalaryTrend(anyInt())).thenReturn(List.of());

        mockMvc.perform(get("/api/dashboard/hr/salary-trend"))
                .andExpect(status().isOk());
    }
}