package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "applications")
@Data
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_posting_id", nullable = false)
    private JobPosting jobPosting;

    private String candidateName;
    private String candidateEmail;
    private String candidatePhone;

    private Boolean meetsRequirements; // null = not yet assessed
    private String outcome; // ACCEPTED, REJECTED, null = not yet decided
    private String outcomeReason;
    private LocalDateTime decidedAt; // when the final accept/reject decision was made
    private String requirementsReason;
    private LocalDateTime reviewedAt;




    private String cvPath;

    @Column(length = 2000)
    private String coverLetter;

    private String status = "SUBMITTED"; // SUBMITTED, REVIEWED, INTERVIEW_SCHEDULED, REJECTED, HIRED
    private LocalDateTime submittedAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ApplicationDocument> documents = new ArrayList<>();
}