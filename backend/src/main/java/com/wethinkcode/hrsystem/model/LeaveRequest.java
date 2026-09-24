package com.wethinkcode.hrsystem.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
@Data
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    private String leaveType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String reason;
    private String attachmentPath;

    private String status = "PENDING";
    private String rejectionReason;

    private String decidedByName;
    private LocalDateTime decidedAt;
}