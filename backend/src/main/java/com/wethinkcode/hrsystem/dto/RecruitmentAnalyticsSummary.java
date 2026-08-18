package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class RecruitmentAnalyticsSummary {
    private int periodDays;
    private long totalApplications;
    private long reviewedCount;
    private long meetsRequirementsCount;
    private double requirementsPassRate;
    private long decidedCount;
    private long acceptedCount;
    private double acceptanceRate;
    private double avgDecisionHours;
}