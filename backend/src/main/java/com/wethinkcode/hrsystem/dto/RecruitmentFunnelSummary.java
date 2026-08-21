package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class RecruitmentFunnelSummary {
    private Map<String, Long> countByStatus; // SUBMITTED, REVIEWED, INTERVIEW_SCHEDULED, REJECTED, HIRED
    private long totalApplications;
}