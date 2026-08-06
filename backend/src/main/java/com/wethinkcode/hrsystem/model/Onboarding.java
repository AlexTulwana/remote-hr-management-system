package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "onboardings")
@Data
public class Onboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    private LocalDate startDate;

    private String status = "IN_PROGRESS"; // IN_PROGRESS, COMPLETE

    @Column(length = 2000)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();
}