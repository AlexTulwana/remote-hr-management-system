package com.wethinkcode.hrsystem.model;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


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

    @Column(length = 2000)
    private String rejectedEmailTemplate;

    @Column(length = 2000)
    private String interviewInviteEmailTemplate;

    @Column(length = 2000)
    private String acceptedEmailTemplate;

    private LocalDate startDate;
    private LocalDate endDate;
    private Integer maxApplications;

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "job_posting_required_documents", joinColumns = @JoinColumn(name = "job_posting_id"))
    @Column(name = "document_type")
    @Enumerated(EnumType.STRING)
    private List<DocumentType> requiredDocuments = new ArrayList<>();
}