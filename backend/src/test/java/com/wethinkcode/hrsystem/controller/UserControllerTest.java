package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CurrentUserService currentUserService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    void me_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void me_authenticated_returnsCurrentUserDetails() throws Exception {
        Branch branch = new Branch();
        branch.setName("Cape Town");

        Employee employee = new Employee();
        employee.setId(5L);
        employee.setEmployeeNumber("EMP-0005");
        employee.setFullName("Emma Employee");
        employee.setPosition("Software Engineer");
        employee.setDepartment("Engineering");
        employee.setBranch(branch);
        employee.setEmploymentStatus("ACTIVE");

        User user = new User();
        user.setId(1L);
        user.setUsername("emptest1");
        user.setRole("EMPLOYEE");
        user.setEmployee(employee);

        when(currentUserService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("emptest1"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.employeeId").value(5))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-0005"))
                .andExpect(jsonPath("$.fullName").value("Emma Employee"))
                .andExpect(jsonPath("$.position").value("Software Engineer"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.branchName").value("Cape Town"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"));
    }
}
