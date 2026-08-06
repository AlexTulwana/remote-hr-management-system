package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DocumentType;
import lombok.Data;
import java.time.LocalDate;
import java.util.List;

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
    private List<DocumentType> requiredDocuments;
}