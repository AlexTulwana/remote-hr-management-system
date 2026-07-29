package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "interviews")
@Data
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    private String type; // IN_PERSON, ONLINE
    private LocalDateTime interviewDateTime;

    private String location; // used if IN_PERSON
    private String meetingLink; // used if ONLINE

    private String status = "SCHEDULED"; // SCHEDULED, COMPLETED, CANCELLED
    private String notes;
}