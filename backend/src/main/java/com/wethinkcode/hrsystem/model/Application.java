package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

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

    private String cvPath;

    @Column(length = 2000)
    private String coverLetter;

    private String status = "SUBMITTED"; // SUBMITTED, REVIEWED, INTERVIEW_SCHEDULED, REJECTED, HIRED
    private LocalDateTime submittedAt;
}