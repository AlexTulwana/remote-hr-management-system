package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class LeaveRequestDto {
    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
}