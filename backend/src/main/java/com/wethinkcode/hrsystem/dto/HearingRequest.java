package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HearingRequest {
    private Long employeeId;
    private String caseType;
    private String description;
    private LocalDateTime hearingDateTime;
    private String meetingLink; // manually pasted for now (Stage 8a)
}