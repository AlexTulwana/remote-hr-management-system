package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.config.RabbitMQConfig;
import com.wethinkcode.hrsystem.dto.ForgotPasswordRequest;
import com.wethinkcode.hrsystem.dto.PasswordResetEvent;
import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import com.wethinkcode.hrsystem.dto.ResetPasswordRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private static final Set<String> VALID_ROLES = Set.of("EMPLOYEE", "MANAGER", "HR", "ADMIN");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final CurrentUserService currentUserService;
    private final RabbitTemplate rabbitTemplate;

    @Value("${app.frontend-base-url:http://localhost:5173}")
    private String frontendBaseUrl = "http://localhost:5173";

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil,
                       CurrentUserService currentUserService, RabbitTemplate rabbitTemplate) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.currentUserService = currentUserService;
        this.rabbitTemplate = rabbitTemplate;
    }

    public User register(RegisterRequest request) {
        if (request.getUsername() == null || request.getUsername().isBlank()) {
            throw new RuntimeException("Username is required");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new RuntimeException("Password is required");
        }
        if (request.getRole() == null || !VALID_ROLES.contains(request.getRole())) {
            throw new RuntimeException("Invalid role: " + request.getRole());
        }
        if ("ADMIN".equals(request.getRole())
                && !"ADMIN".equals(currentUserService.getCurrentUser().getRole())) {
            throw new AccessDeniedException("Only Admin can create Admin accounts");
        }
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username is already taken");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        return userRepository.save(user);
    }

    public String login(LoginRequest request) {
        User user = findUserByAnyIdentifier(request.getUsername());

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        if (user.getEmployee() != null && !user.getEmployee().isActive()) {
            throw new RuntimeException("Invalid username or password");
        }

        return jwtUtil.generateToken(user.getUsername(), user.getRole());
    }

    private User findUserByAnyIdentifier(String identifier) {
        return userRepository.findByUsername(identifier)
                .or(() -> userRepository.findByEmployeeEmployeeNumber(identifier))
                .or(() -> userRepository.findByEmployeeIdNumber(identifier))
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));
    }

    public String forgotPassword(ForgotPasswordRequest request) {
        User user = findUserByAnyIdentifier(request.getUsername());

        String token = UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        user.setResetToken(token);
        user.setResetTokenExpiry(expiry);
        userRepository.save(user);

        String email = user.getEmployee() != null ? user.getEmployee().getEmail() : null;
        if (email != null && !email.isBlank()) {
            String fullName = user.getEmployee() != null ? user.getEmployee().getFullName() : user.getUsername();
            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, "password.reset",
                    new PasswordResetEvent(user.getUsername(), fullName, email, token, expiry));
        } else {
            System.out.println("PASSWORD RESET REQUESTED for " + user.getUsername()
                    + " but no email on file. Reset token (valid 15 min): " + token);
        }

        return "If the username exists, a reset token has been generated.";
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().isBlank()) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        User user = userRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Invalid or expired reset token");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }
}