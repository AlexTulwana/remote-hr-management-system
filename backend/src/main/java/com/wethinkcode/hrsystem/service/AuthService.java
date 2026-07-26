package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ForgotPasswordRequest;
import com.wethinkcode.hrsystem.dto.LoginRequest;
import com.wethinkcode.hrsystem.dto.RegisterRequest;
import com.wethinkcode.hrsystem.dto.ResetPasswordRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.JwtUtil;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    public User register(RegisterRequest request) {
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
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusMinutes(15));
        userRepository.save(user);

        // Placeholder — Phase 16 (Notifications) will replace this with a real email.
        System.out.println("PASSWORD RESET REQUESTED for " + user.getUsername()
                + ". Reset token (valid 15 min): " + token);

        return "If the username exists, a reset token has been generated.";
    }

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findAll().stream()
                .filter(u -> request.getToken().equals(u.getResetToken()))
                .findFirst()
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