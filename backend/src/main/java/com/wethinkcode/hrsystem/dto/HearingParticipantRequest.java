package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class HearingParticipantRequest {
    private Long userId;
    private String role; // EMPLOYEE, MANAGER, SUPERVISOR, WITNESS, HR
}