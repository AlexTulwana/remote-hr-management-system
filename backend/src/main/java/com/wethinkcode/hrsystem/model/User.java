package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;

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
    private String password;

    @Column(nullable = false)
    private String role; // EMPLOYEE, MANAGER, HR, ADMIN

    @OneToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;
}
