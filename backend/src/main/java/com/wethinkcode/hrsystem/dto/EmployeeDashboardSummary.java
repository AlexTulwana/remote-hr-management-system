package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class EmployeeDashboardSummary {
    private String employeeNumber;
    private String fullName;
    private String position;
    private String department;
    private String branchName;
    private String employmentStatus;
    private long pendingLeaveRequests;
    private long approvedLeaveRequests;
    private long rejectedLeaveRequests;
    private long attendanceRecordsThisMonth;
    private long pendingRequestsCount;
    private long openDisciplinaryCasesCount;
}