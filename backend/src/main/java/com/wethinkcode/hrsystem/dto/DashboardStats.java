package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import lombok.AllArgsConstructor;

import java.util.Map;

@Data
@AllArgsConstructor
public class DashboardStats {
    private long totalEmployees;
    private Map<String, Long> employeesPerBranch;
    private long pendingLeaveRequests;
    private long approvedLeaveRequests;
    private long rejectedLeaveRequests;
    private long totalAttendanceRecordsToday;
}