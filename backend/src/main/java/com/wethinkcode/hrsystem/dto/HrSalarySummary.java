package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class HrSalarySummary {
    private double totalSalary;
    private double averageSalary;
    private long employeeCount;
    private Map<String, Double> totalSalaryByDepartment;
}