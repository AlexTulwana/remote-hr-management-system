package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.RequestAnalyticsSummary;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.RequestAnalyticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RequestAnalyticsController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class RequestAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RequestAnalyticsService requestAnalyticsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private RequestAnalyticsSummary summary(int periodDays) {
        RequestAnalyticsSummary s = new RequestAnalyticsSummary();
        s.setPeriodDays(periodDays);
        s.setTotalResolved(10);
        s.setApproved(7);
        s.setRejected(2);
        s.setEscalated(1);
        s.setApprovalRate(0.7);
        s.setEscalationRate(0.1);
        s.setAvgResolutionHours(12.5);
        return s;
    }

    @Test
    void getSummary_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/analytics/requests/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSummary_hrRole_isAllowed() throws Exception {
        when(requestAnalyticsService.getSummary(anyInt())).thenReturn(summary(30));

        mockMvc.perform(get("/api/analytics/requests/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(30))
                .andExpect(jsonPath("$.totalResolved").value(10));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_adminRole_isAllowed() throws Exception {
        when(requestAnalyticsService.getSummary(anyInt())).thenReturn(summary(30));

        mockMvc.perform(get("/api/analytics/requests/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSummary_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/requests/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getSummary_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/requests/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSummary_customPeriodDays_isAccepted() throws Exception {
        when(requestAnalyticsService.getSummary(60)).thenReturn(summary(60));

        mockMvc.perform(get("/api/analytics/requests/summary?periodDays=60"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(60));
    }
}
