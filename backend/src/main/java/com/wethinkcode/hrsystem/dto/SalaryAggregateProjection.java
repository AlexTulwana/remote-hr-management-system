package com.wethinkcode.hrsystem.dto;

import java.math.BigDecimal;

public interface SalaryAggregateProjection {
    Long getBranchId();
    String getDepartment();
    BigDecimal getTotalSalary();
    BigDecimal getAverageSalary();
    Long getEmployeeCount();
}