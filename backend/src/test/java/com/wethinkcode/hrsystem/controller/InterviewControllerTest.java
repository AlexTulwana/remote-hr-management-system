package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.model.Interview;
import com.wethinkcode.hrsystem.service.InterviewService;
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

@WebMvcTest(InterviewController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private InterviewService interviewService;

    private static final String INTERVIEW_JSON =
            "{\"applicationId\":10,\"interviewDateTime\":\"2026-02-01T10:00:00\",\"type\":\"IN_PERSON\",\"location\":\"Room 1\"}";

    @Test
    @WithMockUser(roles = "HR")
    void schedule_asHr_returns200() throws Exception {
        Interview interview = new Interview();
        interview.setId(1L);

        when(interviewService.schedule(any())).thenReturn(interview);

        mockMvc.perform(post("/api/interviews")
                        .contentType("application/json")
                        .content(INTERVIEW_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void schedule_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/interviews")
                        .contentType("application/json")
                        .content(INTERVIEW_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void schedule_unauthenticated_returns403() throws Exception {
        mockMvc.perform(post("/api/interviews")
                        .contentType("application/json")
                        .content(INTERVIEW_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void updateStatus_asHr_returns200() throws Exception {
        Interview interview = new Interview();
        interview.setId(1L);
        interview.setStatus("COMPLETED");

        when(interviewService.updateStatus(eq(1L), anyString(), anyString())).thenReturn(interview);

        mockMvc.perform(patch("/api/interviews/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"COMPLETED\",\"notes\":\"went well\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateStatus_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/interviews/1/status")
                        .contentType("application/json")
                        .content("{\"status\":\"COMPLETED\",\"notes\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getById_asHr_returns200() throws Exception {
        Interview interview = new Interview();
        interview.setId(1L);

        when(interviewService.getById(1L)).thenReturn(interview);

        mockMvc.perform(get("/api/interviews/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/interviews/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getByApplication_asHr_returns200() throws Exception {
        when(interviewService.getByApplication(10L)).thenReturn(List.of(new Interview()));

        mockMvc.perform(get("/api/interviews/application/10"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_asHr_returns200() throws Exception {
        when(interviewService.getAll()).thenReturn(List.of(new Interview()));

        mockMvc.perform(get("/api/interviews"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/interviews"))
                .andExpect(status().isForbidden());
    }
}