package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "offboardings")
@Data
public class Offboarding {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "performed_by", nullable = false)
    private User performedBy;

    private String type; // RESIGNATION, TERMINATION

    private LocalDate effectiveDate;

    @Column(length = 2000)
    private String reason;

    private String status = "IN_PROGRESS"; // IN_PROGRESS, SCHEDULED, COMPLETE

    private LocalDateTime createdAt = LocalDateTime.now();
}