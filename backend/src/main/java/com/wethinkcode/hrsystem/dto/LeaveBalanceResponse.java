package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LeaveBalanceResponse {
    private Long employeeId;
    private String employeeNumber;
    private String fullName;
    private String branchName;
    private int year;
    private int allowance;
    private int used;
    private int reserved;
    private int remaining;
    private int otherDaysTaken;
}
