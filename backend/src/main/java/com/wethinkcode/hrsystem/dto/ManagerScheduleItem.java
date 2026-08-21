package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ManagerScheduleItem {
    private String type; // CALENDAR_EVENT, HEARING
    private String title;
    private LocalDateTime dateTime;
    private String location; // meeting link or physical location, may be null
}