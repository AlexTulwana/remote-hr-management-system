package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class EmailTemplateUpdateRequest {
    private String rejectedEmailTemplate;
    private String interviewInviteEmailTemplate;
    private String acceptedEmailTemplate;
}