package com.wethinkcode.hrsystem.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class AnnouncementRequest {
    private String title;
    private String content;
    private String category;
    private LocalDate expiryDate;
    private List<Long> branchIds; // null or empty = everyone (HR/ADMIN only); a manager must list exactly their own branch
}