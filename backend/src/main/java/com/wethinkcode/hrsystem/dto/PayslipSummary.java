package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PayslipSummary {
    private Long id;
    private String payPeriod;
    private LocalDate uploadedDate;
    private EmployeeSummary employee;

    public static PayslipSummary from(com.wethinkcode.hrsystem.model.Payslip p) {
        return new PayslipSummary(
                p.getId(),
                p.getPayPeriod(),
                p.getUploadedDate(),
                p.getEmployee() != null ? EmployeeSummary.from(p.getEmployee()) : null
        );
    }
}
