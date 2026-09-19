package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.EmployeeService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

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
    private UserRepository userRepository;

    @MockitoBean
    private JwtUtil jwtUtil;

    @MockitoBean
    private EmployeeService employeeService;

    private User buildUser(Long id, String username, Long employeeId, String fullName, String position, String branchName) {
        Branch branch = null;
        if (branchName != null) {
            branch = new Branch();
            branch.setName(branchName);
        }

        Employee employee = new Employee();
        employee.setId(employeeId);
        employee.setEmployeeNumber("EMP-" + employeeId);
        employee.setFullName(fullName);
        employee.setPosition(position);
        employee.setDepartment("Engineering");
        employee.setBranch(branch);
        employee.setEmploymentStatus("ACTIVE");
        employee.setContactDetails("082 123 4567");
        employee.setEmail("emma@example.com");

        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole("EMPLOYEE");
        user.setEmployee(employee);
        return user;
    }

    // ---- me() ----

    @Test
    void me_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void me_authenticated_returnsCurrentUserDetails() throws Exception {
        User user = buildUser(1L, "emptest1", 5L, "Emma Employee", "Software Engineer", "Cape Town");
        when(currentUserService.getCurrentUser()).thenReturn(user);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.username").value("emptest1"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andExpect(jsonPath("$.employeeId").value(5))
                .andExpect(jsonPath("$.employeeNumber").value("EMP-5"))
                .andExpect(jsonPath("$.fullName").value("Emma Employee"))
                .andExpect(jsonPath("$.position").value("Software Engineer"))
                .andExpect(jsonPath("$.department").value("Engineering"))
                .andExpect(jsonPath("$.branchName").value("Cape Town"))
                .andExpect(jsonPath("$.employmentStatus").value("ACTIVE"))
                .andExpect(jsonPath("$.contactDetails").value("082 123 4567"))
                .andExpect(jsonPath("$.email").value("emma@example.com"));
    }

    // ---- lookup() ----

    @Test
    void lookup_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(get("/api/users/lookup").param("query", "Emma"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void lookup_returnsMatchingUsers_excludingSelf() throws Exception {
        User self = buildUser(1L, "emptest1", 5L, "Emma Employee", "Software Engineer", "Cape Town");
        User other = buildUser(2L, "emmaadmin", 6L, "Emma Admin", "HR Officer", "Durban");

        when(currentUserService.getCurrentUser()).thenReturn(self);
        when(userRepository.findByEmployeeFullNameContainingIgnoreCase("Emma"))
                .thenReturn(List.of(self, other));

        mockMvc.perform(get("/api/users/lookup").param("query", "Emma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(2))
                .andExpect(jsonPath("$[0].fullName").value("Emma Admin"))
                .andExpect(jsonPath("$[0].position").value("HR Officer"))
                .andExpect(jsonPath("$[0].branchName").value("Durban"));
    }
}
