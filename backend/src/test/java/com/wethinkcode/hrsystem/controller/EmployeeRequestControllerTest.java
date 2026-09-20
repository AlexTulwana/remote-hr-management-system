package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.EmployeeRequestSubmission;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeRequestController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EmployeeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EmployeeRequestService employeeRequestService;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- submit: any authenticated user ----

    @Test
    void submit_unauthenticated_isRejected() throws Exception {
        EmployeeRequestSubmission submission = new EmployeeRequestSubmission();

        mockMvc.perform(post("/api/employee-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void submit_employeeRole_isAllowed() throws Exception {
        EmployeeRequest saved = new EmployeeRequest();
        saved.setId(1L);
        when(employeeRequestService.submit(any(EmployeeRequestSubmission.class), anyString())).thenReturn(saved);

        EmployeeRequestSubmission submission = new EmployeeRequestSubmission();

        mockMvc.perform(post("/api/employee-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(submission)))
                .andExpect(status().isCreated());
    }

    // ---- manager-decision (MANAGER only) ----

    @Test
    @WithMockUser(roles = "HR")
    void managerDecision_hrRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/employee-requests/1/manager-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mgrtest1", roles = "MANAGER")
    void managerDecision_managerRole_isAllowed() throws Exception {
        EmployeeRequest updated = new EmployeeRequest();
        updated.setId(1L);
        when(employeeRequestService.managerDecision(anyLong(), anyString(), any(), anyString())).thenReturn(updated);

        mockMvc.perform(patch("/api/employee-requests/1/manager-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
                .andExpect(status().isOk());
    }

    // ---- hr-decision (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void hrDecision_managerRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/employee-requests/1/hr-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void hrDecision_hrRole_isAllowed() throws Exception {
        EmployeeRequest updated = new EmployeeRequest();
        updated.setId(1L);
        when(employeeRequestService.hrDecision(anyLong(), anyString(), any(), anyString())).thenReturn(updated);

        mockMvc.perform(patch("/api/employee-requests/1/hr-decision")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("decision", "APPROVED"))))
                .andExpect(status().isOk());
    }

    // ---- getById: no @PreAuthorize, access checked in service ----

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getById_allowedByService_returnsOk() throws Exception {
        EmployeeRequest request = new EmployeeRequest();
        request.setId(1L);
        when(employeeRequestService.getById(anyLong(), anyString())).thenReturn(request);

        mockMvc.perform(get("/api/employee-requests/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getById_deniedByService_returnsForbidden() throws Exception {
        when(employeeRequestService.getById(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Not authorized to view this employee request"));

        mockMvc.perform(get("/api/employee-requests/1"))
                .andExpect(status().isForbidden());
    }

    // ---- getByEmployee: no @PreAuthorize, access checked in service ----

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getByEmployee_allowedByService_returnsOk() throws Exception {
        when(employeeRequestService.getByEmployee(anyLong(), anyString())).thenReturn(List.of(new EmployeeRequest()));

        mockMvc.perform(get("/api/employee-requests/employee/4"))
                .andExpect(status().isOk());
    }

    // ---- getByBranch (MANAGER/HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByBranch_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employee-requests/branch/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerRole_isAllowed() throws Exception {
        when(employeeRequestService.getByBranch(anyLong(), anyString())).thenReturn(List.of(new EmployeeRequest()));

        mockMvc.perform(get("/api/employee-requests/branch/1"))
                .andExpect(status().isOk());
    }

    // ---- getAll (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employee-requests"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(employeeRequestService.getAll()).thenReturn(List.of(new EmployeeRequest()));

        mockMvc.perform(get("/api/employee-requests"))
                .andExpect(status().isOk());
    }

    // ---- getEscalated (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void getEscalated_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employee-requests/escalated"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getEscalated_adminRole_isAllowed() throws Exception {
        when(employeeRequestService.getEscalated()).thenReturn(List.of(new EmployeeRequest()));

        mockMvc.perform(get("/api/employee-requests/escalated"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerOtherBranch_deniedByService() throws Exception {
        when(employeeRequestService.getByBranch(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("You can only view requests for your own branch"));

        mockMvc.perform(get("/api/employee-requests/branch/2"))
                .andExpect(status().isForbidden());
    }

    // ---- escalation comment visibility ----

    private EmployeeRequest escalatedRequest() {
        EmployeeRequest request = new EmployeeRequest();
        request.setId(1L);
        request.setStatus("ESCALATED");
        request.setEscalationComment("HR only note");
        return request;
    }

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getById_employeeRole_doesNotSeeEscalationComment() throws Exception {
        when(employeeRequestService.getById(anyLong(), anyString())).thenReturn(escalatedRequest());

        mockMvc.perform(get("/api/employee-requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalationComment").isEmpty());
    }

    @Test
    @WithMockUser(username = "mgrtest1", roles = "MANAGER")
    void getById_managerRole_seesEscalationComment() throws Exception {
        when(employeeRequestService.getById(anyLong(), anyString())).thenReturn(escalatedRequest());

        mockMvc.perform(get("/api/employee-requests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.escalationComment").value("HR only note"));
    }

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getByEmployee_employeeRole_doesNotSeeEscalationComment() throws Exception {
        when(employeeRequestService.getByEmployee(anyLong(), anyString()))
                .thenReturn(List.of(escalatedRequest()));

        mockMvc.perform(get("/api/employee-requests/employee/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].escalationComment").isEmpty());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getEscalated_hrRole_seesEscalationComment() throws Exception {
        when(employeeRequestService.getEscalated()).thenReturn(List.of(escalatedRequest()));

        mockMvc.perform(get("/api/employee-requests/escalated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].escalationComment").value("HR only note"));
    }
}
