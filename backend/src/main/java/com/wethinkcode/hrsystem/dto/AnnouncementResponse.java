package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Announcement;

import java.time.LocalDate;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        String category,
        boolean hasPoster,
        LocalDate postedDate,
        LocalDate expiryDate,
        String postedByName,
        Long branchId,
        String branchName
) {
    private static String posterNameOf(Announcement a) {
        if (a.getPostedBy() == null) return null;
        var employee = a.getPostedBy().getEmployee();
        return employee != null ? employee.getFullName() : a.getPostedBy().getUsername();
    }

    public static AnnouncementResponse from(Announcement a) {
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getCategory(),
                a.getPosterImagePath() != null,
                a.getPostedDate(),
                a.getExpiryDate(),
                posterNameOf(a),
                a.getBranch() != null ? a.getBranch().getId() : null,
                a.getBranch() != null ? a.getBranch().getName() : null
        );
    }
}
