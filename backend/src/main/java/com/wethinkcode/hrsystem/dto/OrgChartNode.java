package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class OrgChartNode {
    private Long employeeId;
    private String fullName;
    private String position;
    private String department;
    private String branchName;
    private List<OrgChartNode> directReports;
}