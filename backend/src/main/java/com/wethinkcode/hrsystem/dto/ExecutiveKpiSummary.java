package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExecutiveKpiSummary {
    private long totalEmployees;
    private long activeEmployees;
    private double turnoverRate30d;
    private long totalPendingItems;
    private long totalApplications;
    private long hiredCount;
    private double totalSalarySpend;
}