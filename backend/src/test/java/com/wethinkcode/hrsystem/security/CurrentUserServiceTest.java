package com.wethinkcode.hrsystem.security;

import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserService currentUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private void loginAs(String role, Employee employee) {
        User user = new User();
        user.setId(1L);
        user.setUsername("tester");
        user.setRole(role);
        user.setEmployee(employee);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("tester", null, List.of()));
        when(userRepository.findByUsername("tester")).thenReturn(Optional.of(user));
    }

    private Employee employeeInBranch(Long branchId) {
        Branch branch = new Branch();
        branch.setId(branchId);
        Employee employee = new Employee();
        employee.setId(10L);
        employee.setBranch(branch);
        return employee;
    }

    // ---- isHrOrAdmin() ----

    @Test
    void isHrOrAdmin_hr_returnsTrue() {
        loginAs("HR", null);
        assertThat(currentUserService.isHrOrAdmin()).isTrue();
    }

    @Test
    void isHrOrAdmin_admin_returnsTrue() {
        loginAs("ADMIN", null);
        assertThat(currentUserService.isHrOrAdmin()).isTrue();
    }

    @Test
    void isHrOrAdmin_manager_returnsFalse() {
        loginAs("MANAGER", null);
        assertThat(currentUserService.isHrOrAdmin()).isFalse();
    }

    @Test
    void isHrOrAdmin_employee_returnsFalse() {
        loginAs("EMPLOYEE", null);
        assertThat(currentUserService.isHrOrAdmin()).isFalse();
    }

    // ---- getCurrentBranchId() ----

    @Test
    void getCurrentBranchId_employeeWithBranch_returnsBranchId() {
        loginAs("MANAGER", employeeInBranch(1L));
        assertThat(currentUserService.getCurrentBranchId()).isEqualTo(1L);
    }

    @Test
    void getCurrentBranchId_employeeWithoutBranch_returnsNull() {
        Employee employee = new Employee();
        employee.setId(10L);
        loginAs("MANAGER", employee);
        assertThat(currentUserService.getCurrentBranchId()).isNull();
    }

    @Test
    void getCurrentBranchId_noLinkedEmployee_returnsNull() {
        loginAs("MANAGER", null);
        assertThat(currentUserService.getCurrentBranchId()).isNull();
    }
}
