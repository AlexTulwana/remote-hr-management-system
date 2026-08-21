package com.wethinkcode.hrsystem.dto;

public interface HeadcountAggregateProjection {
    Long getBranchId();
    String getDepartment();
    String getEmploymentStatus();
    Long getHeadcount();
}