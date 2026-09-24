package com.wethinkcode.hrsystem.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class OnboardingFromApplicationRequest {
    private String role;
    private LocalDate startDate;
    private Long reportsToId;
    private String notes;
}
