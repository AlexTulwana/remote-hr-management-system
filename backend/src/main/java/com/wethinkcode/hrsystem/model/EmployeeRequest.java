package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "employee_requests")
@Data
public class EmployeeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee; // who submitted the request

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestType requestType;

    @Column(length = 2000)
    private String description;

    private LocalDateTime submittedAt;

    private String status = "PENDING"; // PENDING, APPROVED, REJECTED, ESCALATED

    @ManyToOne
    @JoinColumn(name = "handled_by")
    private User handledBy; // last Manager/HR user to act on this request

    @Column(length = 1000)
    private String managerComment;

    @Column(length = 1000)
    private String hrComment;

    @Column(length = 1000)
    private String escalationComment; // for HR only, never shown to the employee

    private LocalDateTime resolvedAt;
}
