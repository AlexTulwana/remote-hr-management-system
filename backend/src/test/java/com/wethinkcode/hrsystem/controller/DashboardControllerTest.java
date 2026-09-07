package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.DashboardStats;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.DashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DashboardController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getStats_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getStats_hrRole_isAllowed() throws Exception {
        DashboardStats stats = new DashboardStats(4, Map.of("Cape Town", 4L), 0, 0, 0, 0);
        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getStats_managerRole_allowedByService() throws Exception {
        DashboardStats stats = new DashboardStats(1, Map.of("Cape Town", 1L), 0, 0, 0, 0);
        when(dashboardService.getStats()).thenReturn(stats);

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getStats_employeeRole_deniedByService() throws Exception {
        when(dashboardService.getStats())
                .thenThrow(new AccessDeniedException("Not authorized to view dashboard stats"));

        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isForbidden());
    }
}