package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;

@Entity
@Table(name = "announcements")
@Data
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 3000)
    private String content;

    private String category; // General, Vacancy, Notice, Advert, Staff Meeting

    private String posterImagePath;

    private LocalDate postedDate;
    private LocalDate expiryDate; // nullable - null means it never auto-expires

    @ManyToOne
    @JoinColumn(name = "posted_by")
    private User postedBy;

    @ManyToOne
    @JoinColumn(name = "branch_id")
    private Branch branch;   // null = visible to all branches
}