package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class SalaryTrendPoint {
    private LocalDate date;
    private double totalSalary;
    private double averageSalary;
}