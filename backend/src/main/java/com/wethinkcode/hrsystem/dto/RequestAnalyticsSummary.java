package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class RequestAnalyticsSummary {
    private int periodDays;
    private long totalResolved;
    private long approved;
    private long rejected;
    private long escalated;
    private double approvalRate;
    private double escalationRate;
    private double avgResolutionHours;
}