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

    private Double salary;
    private String payGrade;
    private String bankingDetails;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;
}