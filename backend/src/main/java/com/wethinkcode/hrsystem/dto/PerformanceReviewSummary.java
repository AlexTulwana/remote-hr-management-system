package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.PerformanceReview;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class PerformanceReviewSummary {
    private Long id;
    private EmployeeSummary employee;
    private String reviewerName;
    private int communicationScore;
    private int teamworkScore;
    private int productivityScore;
    private int attendanceScore;
    private double averageScore;
    private String comment;
    private LocalDate reviewDate;

    public static PerformanceReviewSummary from(PerformanceReview r) {
        double average = (r.getCommunicationScore() + r.getTeamworkScore()
                + r.getProductivityScore() + r.getAttendanceScore()) / 4.0;
        String reviewerName = null;
        if (r.getReviewer() != null) {
            reviewerName = r.getReviewer().getEmployee() != null && r.getReviewer().getEmployee().getFullName() != null
                    ? r.getReviewer().getEmployee().getFullName()
                    : r.getReviewer().getUsername();
        }
        return new PerformanceReviewSummary(
                r.getId(),
                r.getEmployee() != null ? EmployeeSummary.from(r.getEmployee()) : null,
                reviewerName,
                r.getCommunicationScore(),
                r.getTeamworkScore(),
                r.getProductivityScore(),
                r.getAttendanceScore(),
                Math.round(average * 10) / 10.0,
                r.getComment(),
                r.getReviewDate()
        );
    }
}
