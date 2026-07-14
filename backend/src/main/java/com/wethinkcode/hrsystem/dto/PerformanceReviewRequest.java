package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class PerformanceReviewRequest {
    private Long employeeId;
    private Long reviewerId;
    private int communicationScore;
    private int teamworkScore;
    private int productivityScore;
    private int attendanceScore;
    private String comment;
}