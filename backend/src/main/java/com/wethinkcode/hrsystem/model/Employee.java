package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "employees")
@Data
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String employeeNumber;

    private String fullName;
    private String position;
    private String department;
    private String qualifications;
    private String contactDetails;
    private LocalDate employmentDate;
    private boolean active = true;
    private String idNumber;

    private Double salary;
    private String payGrade;
    private String bankingDetails;

    private String employmentStatus = "ACTIVE"; // ACTIVE, ONBOARDING, RESIGNED, TERMINATED
    private LocalDate resignationDate;
    private LocalDate terminationDate;
    private String email;

    @ManyToOne
    @JoinColumn(name = "reports_to_id")
    private Employee reportsTo;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;
}