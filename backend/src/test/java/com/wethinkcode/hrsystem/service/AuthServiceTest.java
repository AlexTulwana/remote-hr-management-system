package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ForgotPasswordRequest;
import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import com.wethinkcode.hrsystem.dto.ResetPasswordRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthService authService;

    private User existingUser;

    @BeforeEach
    void setUp() {
        existingUser = new User();
        existingUser.setId(1L);
        existingUser.setUsername("hrtest2");
        existingUser.setPassword("encoded-password");
        existingUser.setRole("HR");
    }

    // --- register ---

    @Test
    void register_encodesPasswordAndSavesUser() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setPassword("plainPassword123");
        request.setRole("EMPLOYEE");

        when(passwordEncoder.encode("plainPassword123")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(request);

        assertThat(result.getUsername()).isEqualTo("newuser");
        assertThat(result.getPassword()).isEqualTo("encoded-password");
        assertThat(result.getRole()).isEqualTo("EMPLOYEE");
        verify(userRepository).save(any(User.class));
    }

    // --- login ---

    @Test
    void login_withValidCredentials_returnsToken() {
        LoginRequest request = new LoginRequest();
        request.setUsername("hrtest2");
        request.setPassword("correctPassword");

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("hrtest2", "HR")).thenReturn("fake-jwt-token");

        String token = authService.login(request);

        assertThat(token).isEqualTo("fake-jwt-token");
    }

    @Test
    void login_withWrongPassword_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("hrtest2");
        request.setPassword("wrongPassword");

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("wrongPassword", "encoded-password")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_withUnknownUsername_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setUsername("doesnotexist");
        request.setPassword("whatever");

        when(userRepository.findByUsername("doesnotexist")).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeEmployeeNumber("doesnotexist")).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeIdNumber("doesnotexist")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid username or password");
    }

    @Test
    void login_byEmployeeNumber_fallsBackCorrectly() {
        LoginRequest request = new LoginRequest();
        request.setUsername("EMP001");
        request.setPassword("correctPassword");

        when(userRepository.findByUsername("EMP001")).thenReturn(Optional.empty());
        when(userRepository.findByEmployeeEmployeeNumber("EMP001")).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches("correctPassword", "encoded-password")).thenReturn(true);
        when(jwtUtil.generateToken("hrtest2", "HR")).thenReturn("fake-jwt-token");

        String token = authService.login(request);

        assertThat(token).isEqualTo("fake-jwt-token");
    }

    // --- forgotPassword ---

    @Test
    void forgotPassword_generatesAndSavesResetToken() {
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setUsername("hrtest2");

        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        String message = authService.forgotPassword(request);

        assertThat(message).isEqualTo("If the username exists, a reset token has been generated.");
        assertThat(existingUser.getResetToken()).isNotNull();
        assertThat(existingUser.getResetTokenExpiry()).isAfter(LocalDateTime.now());
        verify(userRepository).save(existingUser);
    }

    // --- resetPassword ---

    @Test
    void resetPassword_withValidToken_updatesPassword() {
        existingUser.setResetToken("valid-token");
        existingUser.setResetTokenExpiry(LocalDateTime.now().plusMinutes(10));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPlainPassword");

        when(userRepository.findAll()).thenReturn(java.util.List.of(existingUser));
        when(passwordEncoder.encode("newPlainPassword")).thenReturn("new-encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword(request);

        assertThat(existingUser.getPassword()).isEqualTo("new-encoded-password");
        assertThat(existingUser.getResetToken()).isNull();
        assertThat(existingUser.getResetTokenExpiry()).isNull();
    }

    @Test
    void resetPassword_withExpiredToken_throwsException() {
        existingUser.setResetToken("expired-token");
        existingUser.setResetTokenExpiry(LocalDateTime.now().minusMinutes(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPlainPassword");

        when(userRepository.findAll()).thenReturn(java.util.List.of(existingUser));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid or expired reset token");
    }

    @Test
    void resetPassword_withUnknownToken_throwsException() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("does-not-exist");
        request.setNewPassword("newPlainPassword");

        when(userRepository.findAll()).thenReturn(java.util.List.of(existingUser));

        assertThatThrownBy(() -> authService.resetPassword(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid or expired reset token");
    }

    // --- register rules ---

    private RegisterRequest registerRequest(String username, String password, String role) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        return request;
    }

    @Test
    void register_invalidRole_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> authService.register(registerRequest("newuser", "pw12345678", "SUPERUSER")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid role: SUPERUSER");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_nullRole_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> authService.register(registerRequest("newuser", "pw12345678", null)))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Invalid role: null");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankUsername_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> authService.register(registerRequest("  ", "pw12345678", "EMPLOYEE")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username is required");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_blankPassword_throwsAndDoesNotSave() {
        assertThatThrownBy(() -> authService.register(registerRequest("newuser", "", "EMPLOYEE")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Password is required");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_duplicateUsername_throwsAndDoesNotSave() {
        when(userRepository.findByUsername("taken")).thenReturn(Optional.of(existingUser));

        assertThatThrownBy(() -> authService.register(registerRequest("taken", "pw12345678", "EMPLOYEE")))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_adminRequestedByHr_throwsAccessDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(existingUser); // role HR

        assertThatThrownBy(() -> authService.register(registerRequest("newadmin", "pw12345678", "ADMIN")))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("Only Admin can create Admin accounts");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_adminRequestedByAdmin_succeeds() {
        User admin = new User();
        admin.setRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(admin);
        when(passwordEncoder.encode("pw12345678")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User result = authService.register(registerRequest("newadmin", "pw12345678", "ADMIN"));

        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    @Test
    void register_nonAdminRole_doesNotNeedCurrentUser() {
        when(passwordEncoder.encode("pw12345678")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(registerRequest("newhr", "pw12345678", "HR"));

        verify(currentUserService, never()).getCurrentUser();
    }
}
