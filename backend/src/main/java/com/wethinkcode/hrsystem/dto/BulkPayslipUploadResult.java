package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BulkPayslipUploadResult {
    private String filename;
    private String employeeNumber;
    private boolean success;
    private String message;
    private PayslipSummary payslip;

    public static BulkPayslipUploadResult success(String filename, String employeeNumber, PayslipSummary payslip) {
        return new BulkPayslipUploadResult(filename, employeeNumber, true, "Uploaded", payslip);
    }

    public static BulkPayslipUploadResult failure(String filename, String employeeNumber, String message) {
        return new BulkPayslipUploadResult(filename, employeeNumber, false, message, null);
    }
}
