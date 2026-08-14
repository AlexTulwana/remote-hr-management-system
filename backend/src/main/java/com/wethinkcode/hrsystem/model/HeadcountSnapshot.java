package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "headcount_snapshots",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"snapshot_date", "branch_id", "department", "employment_status"}
        ))
@Data
public class HeadcountSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private String department;

    @Column(name = "employment_status", nullable = false)
    private String employmentStatus;

    @Column(nullable = false)
    private Integer headcount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}