package com.wethinkcode.hrsystem.controller;
import org.springframework.context.annotation.Import;

import com.wethinkcode.hrsystem.model.JobPosting;
import com.wethinkcode.hrsystem.service.JobPostingService;
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

@WebMvcTest(JobPostingController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class JobPostingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private JobPostingService jobPostingService;

    private static final String POSTING_JSON =
            "{\"title\":\"Backend Developer\",\"description\":\"Build APIs\",\"requirements\":\"Java\"," +
                    "\"department\":\"IT\",\"startDate\":\"2026-01-01\",\"endDate\":\"2026-02-01\"," +
                    "\"maxApplications\":50,\"postedById\":1}";

    // ---------- getOpen (public) ----------

    @Test
    void getOpen_unauthenticated_returns200() throws Exception {
        when(jobPostingService.getOpen()).thenReturn(List.of(new JobPosting()));

        mockMvc.perform(get("/api/job-postings"))
                .andExpect(status().isOk());
    }

    // ---------- getById (public) ----------

    @Test
    void getById_unauthenticated_returns200() throws Exception {
        JobPosting posting = new JobPosting();
        posting.setId(1L);

        when(jobPostingService.getById(1L)).thenReturn(posting);

        mockMvc.perform(get("/api/job-postings/1"))
                .andExpect(status().isOk());
    }

    // ---------- getAll ----------

    @Test
    @WithMockUser(roles = "HR")
    void getAll_asHr_returns200() throws Exception {
        when(jobPostingService.getAll()).thenReturn(List.of(new JobPosting()));

        mockMvc.perform(get("/api/job-postings/all"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_asEmployee_returns403() throws Exception {
        mockMvc.perform(get("/api/job-postings/all"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAll_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get("/api/job-postings/all"))
                .andExpect(status().isForbidden());
    }

    // ---------- create ----------

    @Test
    @WithMockUser(roles = "HR")
    void create_asHr_returns200() throws Exception {
        JobPosting posting = new JobPosting();
        posting.setId(1L);

        when(jobPostingService.create(any())).thenReturn(posting);

        mockMvc.perform(post("/api/job-postings")
                        .contentType("application/json")
                        .content(POSTING_JSON))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_asEmployee_returns403() throws Exception {
        mockMvc.perform(post("/api/job-postings")
                        .contentType("application/json")
                        .content(POSTING_JSON))
                .andExpect(status().isForbidden());
    }

    // ---------- update ----------

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_returns200() throws Exception {
        JobPosting posting = new JobPosting();
        posting.setId(5L);

        when(jobPostingService.update(eq(5L), any())).thenReturn(posting);

        mockMvc.perform(patch("/api/job-postings/5")
                        .contentType("application/json")
                        .content(POSTING_JSON))
                .andExpect(status().isOk());
    }

    // ---------- updateEmailTemplates ----------

    @Test
    @WithMockUser(roles = "HR")
    void updateEmailTemplates_asHr_returns200() throws Exception {
        JobPosting posting = new JobPosting();
        posting.setId(5L);

        when(jobPostingService.updateEmailTemplates(eq(5L), any())).thenReturn(posting);

        mockMvc.perform(patch("/api/job-postings/5/email-templates")
                        .contentType("application/json")
                        .content("{\"rejectedEmailTemplate\":\"x\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void updateEmailTemplates_asEmployee_returns403() throws Exception {
        mockMvc.perform(patch("/api/job-postings/5/email-templates")
                        .contentType("application/json")
                        .content("{\"rejectedEmailTemplate\":\"x\"}"))
                .andExpect(status().isForbidden());
    }

    // ---------- delete ----------

    @Test
    @WithMockUser(roles = "HR")
    void delete_asHr_returns204() throws Exception {
        mockMvc.perform(delete("/api/job-postings/5"))
                .andExpect(status().isNoContent());

        verify(jobPostingService).delete(5L);
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void delete_asEmployee_returns403() throws Exception {
        mockMvc.perform(delete("/api/job-postings/5"))
                .andExpect(status().isForbidden());
    }
}