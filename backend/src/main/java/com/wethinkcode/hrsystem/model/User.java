package com.wethinkcode.hrsystem.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Column(nullable = false)
    private String role; // EMPLOYEE, MANAGER, HR, ADMIN

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String resetToken;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private LocalDateTime resetTokenExpiry;
}