package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

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

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "announcement_branches",
            joinColumns = @JoinColumn(name = "announcement_id"),
            inverseJoinColumns = @JoinColumn(name = "branch_id"))
    private Set<Branch> branches = new HashSet<>();   // empty = visible to everyone
}