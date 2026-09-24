package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Offboarding;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.OffboardingService;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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

@WebMvcTest(OffboardingController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class OffboardingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OffboardingService offboardingService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static final String START_BODY = """
            {
              "type": "RESIGNATION",
              "effectiveDate": "2026-09-15",
              "reason": "better opportunity"
            }
            """;

    // ---- start() ----

    @Test
    void start_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/offboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void start_hrRole_isAllowed() throws Exception {
        when(offboardingService.start(anyLong(), anyString(), any(), anyString()))
                .thenReturn(new Offboarding());

        mockMvc.perform(post("/api/offboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void start_adminRole_isAllowed() throws Exception {
        when(offboardingService.start(anyLong(), anyString(), any(), anyString()))
                .thenReturn(new Offboarding());

        mockMvc.perform(post("/api/offboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void start_managerRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/offboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void start_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/offboarding/1")
                        .contentType("application/json")
                        .content(START_BODY))
                .andExpect(status().isForbidden());
    }

    // ---- complete() ----

    @Test
    void complete_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(patch("/api/offboarding/5/complete"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void complete_hrRole_isAllowed() throws Exception {
        when(offboardingService.complete(5L)).thenReturn(new Offboarding());

        mockMvc.perform(patch("/api/offboarding/5/complete"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void complete_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/offboarding/5/complete"))
                .andExpect(status().isForbidden());
    }

    // ---- getById() ----

    @Test
    void getById_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/offboarding/7"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getById_hrRole_isAllowed() throws Exception {
        when(offboardingService.getById(7L)).thenReturn(new Offboarding());

        mockMvc.perform(get("/api/offboarding/7"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getById_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/offboarding/7"))
                .andExpect(status().isForbidden());
    }

    // ---- getByEmployee() ----

    @Test
    @WithMockUser(roles = "HR")
    void getByEmployee_hrRole_isAllowed() throws Exception {
        when(offboardingService.getByEmployee(1L)).thenReturn(List.of(new Offboarding()));

        mockMvc.perform(get("/api/offboarding/employee/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/offboarding/employee/1"))
                .andExpect(status().isForbidden());
    }

    // ---- getAll() ----

    @Test
    void getAll_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/offboarding"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(offboardingService.getAll()).thenReturn(List.of(new Offboarding(), new Offboarding()));

        mockMvc.perform(get("/api/offboarding"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/offboarding"))
                .andExpect(status().isForbidden());
    }

    // ---- leak regression: no Employee/User internals in the response ----

    @Test
    @WithMockUser(roles = "HR")
    void getById_doesNotLeakSensitiveFields() throws Exception {
        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Emma Employee");
        employee.setSalary(98765.0);
        employee.setBankingDetails("BANK-SECRET-123");
        employee.setIdNumber("ID-SECRET-456");

        User performer = new User();
        performer.setUsername("hrtest1");
        performer.setPassword("PASSWORD-HASH-789");
        performer.setResetToken("RESET-TOKEN-000");

        Offboarding record = new Offboarding();
        record.setId(10L);
        record.setEmployee(employee);
        record.setPerformedBy(performer);
        record.setType("RESIGNATION");
        when(offboardingService.getById(10L)).thenReturn(record);

        mockMvc.perform(get("/api/offboarding/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employeeName").value("Emma Employee"))
                .andExpect(jsonPath("$.performedBy").value("hrtest1"))
                .andExpect(jsonPath("$.employee").doesNotExist())
                .andExpect(content().string(not(containsString("98765"))))
                .andExpect(content().string(not(containsString("BANK-SECRET-123"))))
                .andExpect(content().string(not(containsString("ID-SECRET-456"))))
                .andExpect(content().string(not(containsString("PASSWORD-HASH-789"))))
                .andExpect(content().string(not(containsString("RESET-TOKEN-000"))));
    }
}
