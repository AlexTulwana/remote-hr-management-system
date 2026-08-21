package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BranchComparisonEntry {
    private Long branchId;
    private String branchName;
    private long activeHeadcount;
    private double totalSalary;
    private double averageSalary;
    private double turnoverRate30d;
}