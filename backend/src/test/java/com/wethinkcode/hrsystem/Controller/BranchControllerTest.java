package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.BranchRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.BranchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BranchController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class BranchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private BranchService branchService;

    @MockitoBean
    private JwtUtil jwtUtil;

    // ---- create (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_employeeRole_isForbidden() throws Exception {
        BranchRequest request = new BranchRequest();

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void create_hrRole_isAllowed() throws Exception {
        Branch created = new Branch();
        created.setId(1L);
        created.setName("Durban");
        when(branchService.create(any(BranchRequest.class))).thenReturn(created);

        BranchRequest request = new BranchRequest();

        mockMvc.perform(post("/api/branches")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- getAll / getById: open to any authenticated user ----

    @Test
    void getAll_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_employeeRole_isAllowed() throws Exception {
        when(branchService.getAll()).thenReturn(List.of(new Branch()));

        mockMvc.perform(get("/api/branches"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getById_employeeRole_isAllowed() throws Exception {
        Branch branch = new Branch();
        branch.setId(1L);
        when(branchService.getById(1L)).thenReturn(branch);

        mockMvc.perform(get("/api/branches/1"))
                .andExpect(status().isOk());
    }

    // ---- update (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_managerRole_isForbidden() throws Exception {
        BranchRequest request = new BranchRequest();

        mockMvc.perform(put("/api/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_adminRole_isAllowed() throws Exception {
        Branch updated = new Branch();
        updated.setId(1L);
        when(branchService.update(anyLong(), any(BranchRequest.class))).thenReturn(updated);

        BranchRequest request = new BranchRequest();

        mockMvc.perform(put("/api/branches/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    // ---- delete (HR/ADMIN) ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void delete_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(delete("/api/branches/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void delete_hrRole_isAllowed() throws Exception {
        mockMvc.perform(delete("/api/branches/1"))
                .andExpect(status().isNoContent());
    }
}