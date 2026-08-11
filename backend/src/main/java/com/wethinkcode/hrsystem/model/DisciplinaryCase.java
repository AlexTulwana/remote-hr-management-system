package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "disciplinary_cases")
@Data
public class DisciplinaryCase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    @Column(length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisciplinaryStage currentStage;

    @ManyToOne
    @JoinColumn(name = "opened_by", nullable = false)
    private User openedBy;

    private LocalDateTime openedAt;

    // Optional - set if this case was formalized from an employee-submitted Escalation
    @ManyToOne
    @JoinColumn(name = "linked_escalation_id")
    private Escalation linkedEscalation;

    private boolean closed = false;
}
