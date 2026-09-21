package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.model.User;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class PerformanceReviewSummaryTest {

    private PerformanceReview review(int communication, int teamwork, int productivity, int attendance) {
        Employee employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
        employee.setSalary(50000.0);

        Employee reviewerEmployee = new Employee();
        reviewerEmployee.setFullName("Sam Manager");
        User reviewer = new User();
        reviewer.setUsername("mgrtest1");
        reviewer.setEmployee(reviewerEmployee);

        PerformanceReview review = new PerformanceReview();
        review.setId(1L);
        review.setEmployee(employee);
        review.setReviewer(reviewer);
        review.setCommunicationScore(communication);
        review.setTeamworkScore(teamwork);
        review.setProductivityScore(productivity);
        review.setAttendanceScore(attendance);
        review.setComment("Solid quarter");
        review.setReviewDate(LocalDate.of(2026, 9, 20));
        return review;
    }

    @Test
    void from_mapsFieldsAndComputesAverage() {
        PerformanceReviewSummary summary = PerformanceReviewSummary.from(review(4, 5, 3, 4));

        assertThat(summary.getEmployee().getFullName()).isEqualTo("Emma Employee");
        assertThat(summary.getReviewerName()).isEqualTo("Sam Manager");
        assertThat(summary.getCommunicationScore()).isEqualTo(4);
        assertThat(summary.getAttendanceScore()).isEqualTo(4);
        assertThat(summary.getAverageScore()).isEqualTo(4.0);
        assertThat(summary.getComment()).isEqualTo("Solid quarter");
        assertThat(summary.getReviewDate()).isEqualTo(LocalDate.of(2026, 9, 20));
    }

    @Test
    void from_roundsAverageToOneDecimal() {
        assertThat(PerformanceReviewSummary.from(review(5, 4, 4, 4)).getAverageScore()).isEqualTo(4.3);
    }

    @Test
    void from_reviewerWithoutEmployee_usesUsername() {
        PerformanceReview review = review(3, 3, 3, 3);
        review.getReviewer().setEmployee(null);

        assertThat(PerformanceReviewSummary.from(review).getReviewerName()).isEqualTo("mgrtest1");
    }
}
