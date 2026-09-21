package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.PerformanceReviewService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PerformanceReviewController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class PerformanceReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PerformanceReviewService reviewService;

    @MockitoBean
    private JwtUtil jwtUtil;

    private static final String CREATE_BODY = """
            {
              "employeeId": 1,
              "communicationScore": 4,
              "teamworkScore": 5,
              "productivityScore": 3,
              "attendanceScore": 5,
              "comment": "Solid quarter overall."
            }
            """;

    // ---- create() ----

    @Test
    void create_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_managerRole_isAllowed() throws Exception {
        when(reviewService.create(any(PerformanceReviewRequest.class), anyString()))
                .thenReturn(new PerformanceReview());

        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "HR")
    void create_hrRole_isAllowed() throws Exception {
        when(reviewService.create(any(PerformanceReviewRequest.class), anyString()))
                .thenReturn(new PerformanceReview());

        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void create_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    // ---- getByEmployee() ----

    @Test
    void getByEmployee_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/reviews/employee/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getByEmployee_hrRole_isAllowed() throws Exception {
        when(reviewService.getByEmployee(1L)).thenReturn(List.of(new PerformanceReview()));

        mockMvc.perform(get("/api/reviews/employee/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByEmployee_employeeViewingOther_deniedByService() throws Exception {
        when(reviewService.getByEmployee(anyLong()))
                .thenThrow(new AccessDeniedException("Not authorized to view this employee's reviews"));

        mockMvc.perform(get("/api/reviews/employee/1"))
                .andExpect(status().isForbidden());
    }

    // ---- getAll() ----

    @Test
    void getAll_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "HR")
    void getAll_hrRole_isAllowed() throws Exception {
        when(reviewService.getAll()).thenReturn(List.of(new PerformanceReview(), new PerformanceReview()));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAll_adminRole_isAllowed() throws Exception {
        when(reviewService.getAll()).thenReturn(List.of(new PerformanceReview()));

        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAll_managerRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getAll_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/reviews"))
                .andExpect(status().isForbidden());
    }

    // ---- getByBranch() ----

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void getByBranch_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(get("/api/reviews/branch/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerRole_isAllowed() throws Exception {
        when(reviewService.getByBranch(1L)).thenReturn(List.of(new PerformanceReview()));

        mockMvc.perform(get("/api/reviews/branch/1"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void getByBranch_managerOtherBranch_deniedByService() throws Exception {
        when(reviewService.getByBranch(anyLong()))
                .thenThrow(new AccessDeniedException("You can only view reviews for your own branch"));

        mockMvc.perform(get("/api/reviews/branch/2"))
                .andExpect(status().isForbidden());
    }

    // ---- create() denial from the service ----

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_managerOtherBranch_deniedByService() throws Exception {
        when(reviewService.create(any(PerformanceReviewRequest.class), anyString()))
                .thenThrow(new AccessDeniedException("Managers can only review employees in their own branch"));

        mockMvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content(CREATE_BODY))
                .andExpect(status().isForbidden());
    }

    // ---- no sensitive data in responses ----

    @Test
    @WithMockUser(roles = "HR")
    void getByEmployee_returnsSummaryWithoutSensitiveEmployeeFields() throws Exception {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setSalary(50000.0);
        employee.setBankingDetails("test-bank-details");
        PerformanceReview review = new PerformanceReview();
        review.setEmployee(employee);
        when(reviewService.getByEmployee(1L)).thenReturn(List.of(review));

        mockMvc.perform(get("/api/reviews/employee/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee.fullName").value("Emma Employee"))
                .andExpect(jsonPath("$[0].employee.salary").doesNotExist())
                .andExpect(jsonPath("$[0].employee.bankingDetails").doesNotExist());
    }
}
