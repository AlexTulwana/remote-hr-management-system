package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HrPendingItemsSummary {
    private List<LeaveRequestSummary> pendingLeave;
    private List<EmployeeRequestSummary> pendingRequests;
    private List<DisciplinaryCaseSummary> openDisciplinaryCases;
    private long pendingApplications;
    private long totalPendingCount;
}