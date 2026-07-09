package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "hearings")
@Data
public class Hearing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private String caseType; // e.g. Absenteeism, Misconduct
    private String description;
    private LocalDateTime hearingDateTime;

    private String meetingLink;

    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED
    private String outcome;
    private String notes;
}