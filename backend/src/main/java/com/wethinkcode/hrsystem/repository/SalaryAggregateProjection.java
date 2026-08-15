package com.wethinkcode.hrsystem.repository;

public interface SalaryAggregateProjection {
    Long getBranchId();
    String getDepartment();
    Double getTotalSalary();
    Double getAverageSalary();
    Long getEmployeeCount();
}