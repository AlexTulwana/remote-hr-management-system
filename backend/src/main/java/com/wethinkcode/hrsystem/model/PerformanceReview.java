package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "performance_reviews")
@Data
public class PerformanceReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    private int communicationScore; // 1-5
    private int teamworkScore;      // 1-5
    private int productivityScore;  // 1-5
    private int attendanceScore;    // 1-5

    @Column(length = 2000)
    private String comment;

    private LocalDate reviewDate;
}