package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.DisciplinaryCaseRequest;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import com.wethinkcode.hrsystem.model.DisciplinaryCaseHistory;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.DisciplinaryCaseService;
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

@WebMvcTest(DisciplinaryCaseController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class DisciplinaryCaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private DisciplinaryCaseService disciplinaryCaseService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- open (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void open_employeeRole_isForbidden() throws Exception {
        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();

        mockMvc.perform(post("/api/disciplinary-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "hrtest2", roles = "HR")
    void open_hrRole_isAllowed() throws Exception {
        DisciplinaryCase saved = newCase();
        saved.setId(1L);
        when(disciplinaryCaseService.open(any(DisciplinaryCaseRequest.class), anyString())).thenReturn(saved);

        DisciplinaryCaseRequest request = new DisciplinaryCaseRequest();

        mockMvc.perform(post("/api/disciplinary-cases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    // ---- progress (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void progress_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/disciplinary-cases/1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stage", "HEARING"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "mgrtest1", roles = "MANAGER")
    void progress_managerRole_isAllowed() throws Exception {
        DisciplinaryCase updated = newCase();
        updated.setId(1L);
        when(disciplinaryCaseService.progressStage(anyLong(), anyString(), any(), any(), anyString()))
                .thenReturn(updated);

        mockMvc.perform(patch("/api/disciplinary-cases/1/progress")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("stage", "HEARING"))))
                .andExpect(status().isOk());
    }

    // ---- getById: no @PreAuthorize, any authenticated user ----

    @Test
    void getById_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/disciplinary-cases/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_employeeRole_isAllowed() throws Exception {
        DisciplinaryCase caseObj = newCase();
        caseObj.setId(1L);
        when(disciplinaryCaseService.getByIdForViewer(anyLong(), anyString())).thenReturn(caseObj);

        mockMvc.perform(get("/api/disciplinary-cases/1"))
                .andExpect(status().isOk());
    }

    // ---- getHistory: no @PreAuthorize, any authenticated user ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getHistory_employeeRole_isAllowed() throws Exception {
        when(disciplinaryCaseService.getHistoryForViewer(anyLong(), anyString())).thenReturn(List.of(new DisciplinaryCaseHistory()));

        mockMvc.perform(get("/api/disciplinary-cases/1/history"))
                .andExpect(status().isOk());
    }

    // ---- getByEmployee: no @PreAuthorize, access logic in service ----

    @Test
    @WithMockUser(username = "emptest1", roles = "EMPLOYEE")
    void getByEmployee_employeeRole_isAllowed() throws Exception {
        when(disciplinaryCaseService.getByEmployeeForViewer(anyLong(), anyString()))
                .thenReturn(List.of(newCase()));

        mockMvc.perform(get("/api/disciplinary-cases/employee/4"))
                .andExpect(status().isOk());
    }

    // ---- getByBranch (HR/ADMIN/MANAGER) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByBranch_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/disciplinary-cases/branch/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerRole_isAllowed() throws Exception {
        when(disciplinaryCaseService.getByBranch(anyLong(), anyString())).thenReturn(List.of(newCase()));

        mockMvc.perform(get("/api/disciplinary-cases/branch/1"))
                .andExpect(status().isOk());
    }

    // ---- getAllOpen (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAllOpen_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/disciplinary-cases/open"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAllOpen_hrRole_isAllowed() throws Exception {
        when(disciplinaryCaseService.getAllOpen()).thenReturn(List.of(newCase()));

        mockMvc.perform(get("/api/disciplinary-cases/open"))
                .andExpect(status().isOk());
    }

    // ---- getAll (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/disciplinary-cases"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(disciplinaryCaseService.getAll()).thenReturn(List.of(newCase()));

        mockMvc.perform(get("/api/disciplinary-cases"))
                .andExpect(status().isOk());
    }

    // ---- fixtures and access / leak tests ----

    private DisciplinaryCase newCase() {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        DisciplinaryCase disciplinaryCase = new DisciplinaryCase();
        disciplinaryCase.setEmployee(employee);
        return disciplinaryCase;
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_deniedByService_returnsForbidden() throws Exception {
        when(disciplinaryCaseService.getByIdForViewer(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Not authorized"));

        mockMvc.perform(get("/api/disciplinary-cases/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getHistory_deniedByService_returnsForbidden() throws Exception {
        when(disciplinaryCaseService.getHistoryForViewer(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("Not authorized"));

        mockMvc.perform(get("/api/disciplinary-cases/1/history"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerOtherBranch_deniedByService() throws Exception {
        when(disciplinaryCaseService.getByBranch(anyLong(), anyString()))
                .thenThrow(new AccessDeniedException("You can only view cases for your own branch"));

        mockMvc.perform(get("/api/disciplinary-cases/branch/2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_returnsSummaryWithoutSensitiveEmployeeFields() throws Exception {
        DisciplinaryCase disciplinaryCase = newCase();
        disciplinaryCase.getEmployee().setSalary(50000.0);
        disciplinaryCase.getEmployee().setBankingDetails("test-bank-details");
        disciplinaryCase.getEmployee().setIdNumber("test-id-number");
        when(disciplinaryCaseService.getByIdForViewer(anyLong(), anyString())).thenReturn(disciplinaryCase);

        mockMvc.perform(get("/api/disciplinary-cases/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.fullName").value("Emma Employee"))
                .andExpect(jsonPath("$.employee.salary").doesNotExist())
                .andExpect(jsonPath("$.employee.bankingDetails").doesNotExist())
                .andExpect(jsonPath("$.employee.idNumber").doesNotExist());
    }
}
