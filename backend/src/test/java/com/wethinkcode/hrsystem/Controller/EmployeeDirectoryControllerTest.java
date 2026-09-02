package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDirectoryEntry;
import com.wethinkcode.hrsystem.dto.OrgChartNode;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeDirectoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmployeeDirectoryController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class EmployeeDirectoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EmployeeDirectoryService employeeDirectoryService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void getDirectory_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/employee-directory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getDirectory_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employee-directory"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getDirectory_managerRole_isAllowed() throws Exception {
        when(employeeDirectoryService.getDirectory(any(), any(), any()))
                .thenReturn(List.of(mock(EmployeeDirectoryEntry.class)));

        mockMvc.perform(get("/api/employee-directory"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getDirectory_hrRole_isAllowed() throws Exception {
        when(employeeDirectoryService.getDirectory(any(), any(), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/employee-directory"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getOrgChart_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/employee-directory/org-chart"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrgChart_adminRole_isAllowed() throws Exception {
        when(employeeDirectoryService.getOrgChart()).thenReturn(List.of(mock(OrgChartNode.class)));

        mockMvc.perform(get("/api/employee-directory/org-chart"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getOrgChartFrom_managerRole_isAllowed() throws Exception {
        when(employeeDirectoryService.getOrgChartFrom(anyLong())).thenReturn(mock(OrgChartNode.class));

        mockMvc.perform(get("/api/employee-directory/org-chart/4"))
                .andExpect(status().isOk());
    }
}