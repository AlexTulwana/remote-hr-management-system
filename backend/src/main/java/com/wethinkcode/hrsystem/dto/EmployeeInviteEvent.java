package com.wethinkcode.hrsystem.dto;

import java.time.LocalDateTime;

public record EmployeeInviteEvent(
        String email,
        String fullName,
        String role,
        String token,
        LocalDateTime expiresAt
) {}
