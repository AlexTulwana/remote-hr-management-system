package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.RecruitmentAnalyticsSummary;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.RecruitmentAnalyticsService;
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

@WebMvcTest(RecruitmentAnalyticsController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class RecruitmentAnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RecruitmentAnalyticsService recruitmentAnalyticsService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private RecruitmentAnalyticsSummary summary(int periodDays) {
        RecruitmentAnalyticsSummary s = new RecruitmentAnalyticsSummary();
        s.setPeriodDays(periodDays);
        s.setTotalApplications(20);
        s.setReviewedCount(15);
        s.setMeetsRequirementsCount(12);
        s.setRequirementsPassRate(0.8);
        s.setDecidedCount(10);
        s.setAcceptedCount(4);
        s.setAcceptanceRate(0.4);
        s.setAvgDecisionHours(18.0);
        return s;
    }

    @Test
    void getSummary_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/analytics/recruitment/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSummary_hrRole_isAllowed() throws Exception {
        when(recruitmentAnalyticsService.getSummary(anyInt())).thenReturn(summary(30));

        mockMvc.perform(get("/api/analytics/recruitment/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(30))
                .andExpect(jsonPath("$.totalApplications").value(20));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getSummary_adminRole_isAllowed() throws Exception {
        when(recruitmentAnalyticsService.getSummary(anyInt())).thenReturn(summary(30));

        mockMvc.perform(get("/api/analytics/recruitment/summary"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getSummary_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/recruitment/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getSummary_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/analytics/recruitment/summary"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getSummary_customPeriodDays_isAccepted() throws Exception {
        when(recruitmentAnalyticsService.getSummary(90)).thenReturn(summary(90));

        mockMvc.perform(get("/api/analytics/recruitment/summary?periodDays=90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodDays").value(90));
    }
}
