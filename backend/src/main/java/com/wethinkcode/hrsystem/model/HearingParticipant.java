package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "hearing_participants")
@Data
public class HearingParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "hearing_id", nullable = false)
    private Hearing hearing;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User person;

    private String role; // EMPLOYEE, MANAGER, SUPERVISOR, WITNESS, HR

    private boolean attended = false;
}