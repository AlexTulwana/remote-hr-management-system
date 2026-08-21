package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Map;

@Data
@AllArgsConstructor
public class ManagerHeadcountSummary {
    private long totalEmployees;
    private long activeEmployees;
    private Map<String, Long> activeByDepartment;
}