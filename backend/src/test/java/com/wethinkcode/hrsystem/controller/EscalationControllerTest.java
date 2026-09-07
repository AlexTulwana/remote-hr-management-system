package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.model.Escalation;
import com.wethinkcode.hrsystem.service.EscalationService;
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

@WebMvcTest(EscalationController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EscalationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private EscalationService escalationService;

    private static final String ESCALATION_JSON =
            "{\"type\":\"MANAGER_ESCALATION\",\"reason\":\"Repeated tardiness\"}";

    @Test
    @WithMockUser(roles = "MANAGER")
    void submit_asManager_returns200() throws Exception {
        Escalation escalation = new Escalation();
        escalation.setId(1L);

        when(escalationService.submit(any())).thenReturn(escalation);

        mockMvc.perform(post("/api/escalations")
                        .contentType("application/json")
                        .content(ESCALATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void submit_asEmployee_returns200() throws Exception {
        Escalation escalation = new Escalation();
        escalation.setId(1L);

        when(escalationService.submit(any())).thenReturn(escalation);

        mockMvc.perform(post("/api/escalations")
                        .contentType("application/json")
                        .content(ESCALATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void submit_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/escalations")
                        .contentType("application/json")
                        .content(ESCALATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_asHr_returns200() throws Exception {
        when(escalationService.getAll()).thenReturn(List.of(new Escalation()));

        mockMvc.perform(get("/api/escalations"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/escalations"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void linkToHearing_asHr_returns200() throws Exception {
        Escalation escalation = new Escalation();
        escalation.setId(1L);

        when(escalationService.linkToHearing(1L, 10L)).thenReturn(escalation);

        mockMvc.perform(patch("/api/escalations/1/link-hearing/10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void linkToHearing_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/escalations/1/link-hearing/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void updateStatus_asHr_returns200() throws Exception {
        Escalation escalation = new Escalation();
        escalation.setId(1L);
        escalation.setStatus("UNDER_REVIEW");

        when(escalationService.updateStatus(eq(1L), anyString())).thenReturn(escalation);

        mockMvc.perform(patch("/api/escalations/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateStatus_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/escalations/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"UNDER_REVIEW\"}"))
                .andExpect(status().isForbidden());
    }
}