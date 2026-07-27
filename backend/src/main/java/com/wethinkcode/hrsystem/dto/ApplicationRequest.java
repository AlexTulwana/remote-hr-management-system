package com.wethinkcode.hrsystem.dto;

import lombok.Data;

@Data
public class ApplicationRequest {
    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;
    private String coverLetter;
}