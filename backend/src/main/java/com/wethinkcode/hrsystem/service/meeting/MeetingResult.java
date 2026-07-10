package com.wethinkcode.hrsystem.service.meeting;

import lombok.Data;
import lombok.AllArgsConstructor;

@Data
@AllArgsConstructor
public class MeetingResult {
    private String joinUrl;
    private String meetingId;
    private boolean success;
    private String errorMessage;
}