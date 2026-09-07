package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.EmployeeRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EmployeeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private EmployeeService employeeService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private Employee sampleEmployee() {
        Branch branch = new Branch();
        branch.setId(1L);
        branch.setName("Cape Town");

        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setBranch(branch);
        employee.setReportsTo(null);
        return employee;
    }

    // ---- create (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_employeeRole_isForbidden() throws Exception {
        EmployeeRequest request = new EmployeeRequest();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void create_hrRole_isAllowed() throws Exception {
        when(employeeService.create(any(EmployeeRequest.class))).thenReturn(sampleEmployee());

        EmployeeRequest request = new EmployeeRequest();

        mockMvc.perform(post("/api/employees")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- getAll (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(employeeService.getAll()).thenReturn(List.of(sampleEmployee()));

        mockMvc.perform(get("/api/employees"))
                .andExpect(status().isOk());
    }

    // ---- getById (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employees/4"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getById_adminRole_isAllowed() throws Exception {
        when(employeeService.getById(4L)).thenReturn(sampleEmployee());

        mockMvc.perform(get("/api/employees/4"))
                .andExpect(status().isOk());
    }

    // ---- update (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_managerRole_isForbidden() throws Exception {
        EmployeeRequest request = new EmployeeRequest();

        mockMvc.perform(put("/api/employees/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void update_hrRole_isAllowed() throws Exception {
        when(employeeService.update(anyLong(), any(EmployeeRequest.class))).thenReturn(sampleEmployee());

        EmployeeRequest request = new EmployeeRequest();

        mockMvc.perform(put("/api/employees/4")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- deactivate (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void deactivate_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/employees/4/deactivate"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void deactivate_hrRole_isAllowed() throws Exception {
        mockMvc.perform(patch("/api/employees/4/deactivate"))
                .andExpect(status().isNoContent());
    }

    // ---- link-user (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void linkUser_managerRole_isForbidden() throws Exception {
        mockMvc.perform(patch("/api/employees/4/link-user/10"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void linkUser_adminRole_isAllowed() throws Exception {
        mockMvc.perform(patch("/api/employees/4/link-user/10"))
                .andExpect(status().isNoContent());
    }
}