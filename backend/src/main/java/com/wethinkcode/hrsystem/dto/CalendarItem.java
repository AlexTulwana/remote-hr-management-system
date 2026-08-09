package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CalendarItem {
    private String sourceType; // CALENDAR_EVENT, LEAVE, INTERVIEW, HEARING, JOB_POSTING_OPEN, JOB_POSTING_CLOSE
    private Long sourceId;
    private String title;
    private String description;
    private LocalDate date;
    private LocalDate endDate;
    private Long branchId;
}
