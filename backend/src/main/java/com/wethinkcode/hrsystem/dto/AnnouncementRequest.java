package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class AnnouncementRequest {
    private String title;
    private String content;
    private String category;
    private LocalDate expiryDate;
    private Long postedById;
}