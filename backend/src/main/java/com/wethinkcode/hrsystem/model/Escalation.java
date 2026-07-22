package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "escalations")
@Data
public class Escalation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter; // the Manager or Employee raising this

    @ManyToOne
    @JoinColumn(name = "about_employee_id")
    private Employee aboutEmployee; // optional - who the report concerns

    private String type; // MANAGER_ESCALATION, EMPLOYEE_COMPLAINT

    @Column(length = 2000)
    private String reason;

    private LocalDateTime submittedAt;
    private String status = "SUBMITTED"; // SUBMITTED, UNDER_REVIEW, HEARING_SCHEDULED, RESOLVED

    @ManyToOne
    @JoinColumn(name = "linked_hearing_id")
    private Hearing linkedHearing; // set once HR schedules a hearing from this
}