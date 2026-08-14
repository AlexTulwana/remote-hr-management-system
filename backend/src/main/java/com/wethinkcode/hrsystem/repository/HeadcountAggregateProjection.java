package com.wethinkcode.hrsystem.repository;

public interface HeadcountAggregateProjection {
    Long getBranchId();
    String getDepartment();
    String getEmploymentStatus();
    Long getHeadcount();
}