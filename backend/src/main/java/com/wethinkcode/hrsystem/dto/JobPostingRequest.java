package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class JobPostingRequest {
    private String title;
    private String description;
    private String requirements;
    private String department;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxApplications;
    private Long postedById;
}