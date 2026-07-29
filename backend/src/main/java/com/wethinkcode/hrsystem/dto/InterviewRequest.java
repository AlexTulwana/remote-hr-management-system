package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InterviewRequest {
    private Long applicationId;
    private String type; // IN_PERSON, ONLINE
    private LocalDateTime interviewDateTime;
    private String location; // for IN_PERSON
    private String meetingLink; // manual override for ONLINE, optional
}