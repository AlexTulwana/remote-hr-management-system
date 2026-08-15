package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class ApplicationOutcomeRequest {
    private Boolean meetsRequirements;
    private String requirementsReason;
    private String outcome; // ACCEPTED or REJECTED
    private String outcomeReason;
}