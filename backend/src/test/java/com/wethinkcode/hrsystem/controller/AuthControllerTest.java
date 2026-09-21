package com.wethinkcode.hrsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wethinkcode.hrsystem.dto.ForgotPasswordRequest;
import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import com.wethinkcode.hrsystem.dto.ResetPasswordRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.security.JwtUtil;
import com.wethinkcode.hrsystem.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(com.wethinkcode.hrsystem.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtUtil jwtUtil;

    @Test
    @WithMockUser(roles = "HR")
    void register_returnsCreatedUser() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Password123!");
        request.setRole("EMPLOYEE");

        User created = new User();
        created.setId(1L);
        created.setUsername("newuser");
        created.setRole("EMPLOYEE");

        when(authService.register(any(RegisterRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("newuser"))
                .andExpect(jsonPath("$.role").value("EMPLOYEE"));
    }

    @Test
    void login_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setUsername("hrtest2");
        request.setPassword("TestPass123!");

        when(authService.login(any(LoginRequest.class))).thenReturn("mock-jwt-token");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"));
    }

    @Test
    void forgotPassword_returnsMessageFromService() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("hrtest2");

        when(authService.forgotPassword(any(ForgotPasswordRequest.class)))
                .thenReturn("Reset instructions sent");

        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Reset instructions sent"));
    }

    @Test
    void resetPassword_callsServiceAndReturnsSuccessMessage() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("some-reset-token");
        request.setNewPassword("NewPassword123!");

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successful"));

        verify(authService).resetPassword(any(ResetPasswordRequest.class));
    }

    @Test
    void logout_returnsSuccessMessage_noServiceCallNeeded() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    // ---- register access ----

    private String registerBody() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("Password123!");
        request.setRole("EMPLOYEE");
        return objectMapper.writeValueAsString(request);
    }

    @Test
    void register_unauthenticated_isRejected() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void register_employeeRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void register_managerRole_isForbidden() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isForbidden());

        verify(authService, never()).register(any(RegisterRequest.class));
    }

    @Test
    @WithMockUser(roles = "HR")
    void register_deniedByService_returnsForbidden() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new AccessDeniedException("Only Admin can create Admin accounts"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody()))
                .andExpect(status().isForbidden());
    }
}
