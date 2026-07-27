package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "job_postings")
@Data
public class JobPosting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 3000)
    private String description;

    @Column(length = 2000)
    private String requirements;

    private String department;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxApplications;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;
}