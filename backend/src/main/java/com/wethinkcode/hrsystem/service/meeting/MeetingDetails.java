package com.wethinkcode.hrsystem.service.meeting;

import lombok.Data;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class MeetingDetails {
    private String employeeName;
    private String caseType;
    private LocalDateTime dateTime;
}