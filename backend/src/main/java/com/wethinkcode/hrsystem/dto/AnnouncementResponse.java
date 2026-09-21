package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Announcement;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

public record AnnouncementResponse(
        Long id,
        String title,
        String content,
        String category,
        boolean hasPoster,
        LocalDate postedDate,
        LocalDate expiryDate,
        String postedByName,
        List<BranchInfo> branches
) {
    public record BranchInfo(Long id, String name) {}

    private static String posterNameOf(Announcement a) {
        if (a.getPostedBy() == null) return null;
        var employee = a.getPostedBy().getEmployee();
        return employee != null ? employee.getFullName() : a.getPostedBy().getUsername();
    }

    public static AnnouncementResponse from(Announcement a) {
        List<BranchInfo> branches = a.getBranches() == null ? List.of() : a.getBranches().stream()
                .map(b -> new BranchInfo(b.getId(), b.getName()))
                .sorted(Comparator.comparing(BranchInfo::name, Comparator.nullsFirst(Comparator.naturalOrder())))
                .toList();
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getContent(),
                a.getCategory(),
                a.getPosterImagePath() != null,
                a.getPostedDate(),
                a.getExpiryDate(),
                posterNameOf(a),
                branches
        );
    }
}
