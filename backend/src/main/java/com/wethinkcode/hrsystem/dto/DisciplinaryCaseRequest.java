package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class DisciplinaryCaseRequest {
    private Long employeeId;
    private String reason;
    private String initialStage; // VERBAL_WARNING, WRITTEN_WARNING, FINAL_WRITTEN_WARNING, HEARING, DISMISSAL
    private Long linkedEscalationId; // optional
}
