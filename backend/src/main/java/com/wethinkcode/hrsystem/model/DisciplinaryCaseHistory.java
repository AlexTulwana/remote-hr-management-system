package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "disciplinary_case_history")
@Data
public class DisciplinaryCaseHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "case_id", nullable = false)
    private DisciplinaryCase disciplinaryCase;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DisciplinaryStage stage;

    @ManyToOne
    @JoinColumn(name = "actioned_by", nullable = false)
    private User actionedBy;

    @Column(length = 1000)
    private String comment;

    private LocalDateTime actionedAt;

    // Optional - set if this stage entry corresponds to a scheduled Hearing
    @ManyToOne
    @JoinColumn(name = "linked_hearing_id")
    private Hearing linkedHearing;
}
