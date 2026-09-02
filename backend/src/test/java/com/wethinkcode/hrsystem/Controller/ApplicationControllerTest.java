package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.ApplicationOutcomeRequest;
import com.wethinkcode.hrsystem.model.Application;
import com.wethinkcode.hrsystem.model.ApplicationDocument;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.ApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ApplicationController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class ApplicationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private ApplicationService applicationService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- PUBLIC: submit ----

    @Test
    void submit_noAuth_isPublic() throws Exception {
        Application created = new Application();
        created.setId(1L);
        when(applicationService.submit(anyLong(), any(), any(), anyMap())).thenReturn(created);

        MockMultipartFile cv = new MockMultipartFile("cv", "cv.pdf", "application/pdf", "fake-pdf".getBytes());

        mockMvc.perform(multipart("/api/applications/1")
                        .file(cv)
                        .param("candidateName", "Jane Candidate")
                        .param("candidateEmail", "jane@example.com"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getDocuments (HR/ADMIN) ----

    @Test
    void getDocuments_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/applications/1/documents"))
                .andExpect(status().isForbidden());   // was isUnauthorized()
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getDocuments_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/1/documents"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getDocuments_hrRole_isAllowed() throws Exception {
        when(applicationService.getDocuments(1L)).thenReturn(List.of(new ApplicationDocument()));

        mockMvc.perform(get("/api/applications/1/documents"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: updateStatus (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateStatus_managerRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "REVIEWED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateStatus_adminRole_isAllowed() throws Exception {
        Application updated = new Application();
        updated.setId(1L);
        updated.setStatus("REVIEWED");
        when(applicationService.updateStatus(anyLong(), anyString())).thenReturn(updated);

        mockMvc.perform(patch("/api/applications/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "REVIEWED"))))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getByJobPosting (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByJobPosting_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/job-posting/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getByJobPosting_hrRole_isAllowed() throws Exception {
        when(applicationService.getByJobPosting(1L)).thenReturn(List.of(new Application()));

        mockMvc.perform(get("/api/applications/job-posting/1"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getByStatus (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "HR")
    void getByStatus_hrRole_isAllowed() throws Exception {
        when(applicationService.getByStatus("SUBMITTED")).thenReturn(List.of(new Application()));

        mockMvc.perform(get("/api/applications/status/SUBMITTED"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getById (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/applications/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_adminRole_isAllowed() throws Exception {
        Application application = new Application();
        application.setId(1L);
        when(applicationService.getById(1L)).thenReturn(application);

        mockMvc.perform(get("/api/applications/1"))
                .andExpect(status().isOk());
    }

    // ---- PROTECTED: getAll (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(applicationService.getAll()).thenReturn(List.of(new Application()));

        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/applications"))
                .andExpect(status().isForbidden());
    }

    // ---- PROTECTED: setOutcome (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "HR")
    void setOutcome_hrRole_isAllowed() throws Exception {
        ApplicationOutcomeRequest request = new ApplicationOutcomeRequest();
        request.setMeetsRequirements(true);
        request.setOutcome("ACCEPTED");
        request.setOutcomeReason("Strong candidate");

        Application updated = new Application();
        updated.setId(1L);
        updated.setOutcome("ACCEPTED");
        when(applicationService.setOutcome(anyLong(), any(ApplicationOutcomeRequest.class))).thenReturn(updated);

        mockMvc.perform(patch("/api/applications/1/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void setOutcome_employeeRole_isForbidden() throws Exception {
        ApplicationOutcomeRequest request = new ApplicationOutcomeRequest();
        request.setOutcome("REJECTED");

        mockMvc.perform(patch("/api/applications/1/outcome")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}