package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Onboarding;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.OnboardingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OnboardingController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class OnboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OnboardingService onboardingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static final String START_BODY = """
            {
              "startDate": "2026-09-01",
              "notes": "Standard onboarding"
            }
            """;

    // ---- start() ----

    @Test
    void start_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/onboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void start_hrRole_isAllowed() throws Exception {
        when(onboardingService.start(anyLong(), any(), anyString())).thenReturn(new Onboarding());

        mockMvc.perform(post("/api/onboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void start_adminRole_isAllowed() throws Exception {
        when(onboardingService.start(anyLong(), any(), anyString())).thenReturn(new Onboarding());

        mockMvc.perform(post("/api/onboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void start_managerRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/onboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void start_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/onboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    // ---- complete() ----

    @Test
    void complete_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(patch("/api/onboarding/10/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void complete_hrRole_isAllowed() throws Exception {
        when(onboardingService.complete(10L)).thenReturn(new Onboarding());

        mockMvc.perform(patch("/api/onboarding/10/complete"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void complete_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/onboarding/10/complete"))
                .andExpect(status().isForbidden());
    }

    // ---- getById() ----

    @Test
    void getById_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/onboarding/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getById_hrRole_isAllowed() throws Exception {
        when(onboardingService.getById(10L)).thenReturn(new Onboarding());

        mockMvc.perform(get("/api/onboarding/10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/onboarding/10"))
                .andExpect(status().isForbidden());
    }

    // ---- getByEmployee() ----

    @Test
    @WithMockUser(roles = "HR")
    void getByEmployee_hrRole_isAllowed() throws Exception {
        when(onboardingService.getByEmployee(1L)).thenReturn(List.of(new Onboarding()));

        mockMvc.perform(get("/api/onboarding/employee/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/onboarding/employee/1"))
                .andExpect(status().isForbidden());
    }

    // ---- getAll() ----

    @Test
    void getAll_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/onboarding"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(onboardingService.getAll()).thenReturn(List.of(new Onboarding(), new Onboarding()));

        mockMvc.perform(get("/api/onboarding"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/onboarding"))
                .andExpect(status().isForbidden());
    }
}
