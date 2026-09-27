package com.wethinkcode.hrsystem.dto;

import java.time.LocalDateTime;

public record PasswordResetEvent(
        String username,
        String fullName,
        String email,
        String token,
        LocalDateTime expiresAt
) {}
