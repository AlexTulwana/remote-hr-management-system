package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "salary_snapshots",
        uniqueConstraints = @UniqueConstraint(
                columnNames = {"snapshot_date", "branch_id", "department"}
        ))
@Data
public class SalarySnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "snapshot_date", nullable = false)
    private LocalDate snapshotDate;

    @Column(name = "branch_id", nullable = false)
    private Long branchId;

    @Column(nullable = false)
    private String department;

    @Column(name = "total_salary", nullable = false)
    private Double totalSalary;

    @Column(name = "average_salary", nullable = false)
    private Double averageSalary;

    @Column(name = "employee_count", nullable = false)
    private Integer employeeCount;
}