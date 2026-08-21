package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.DisciplinaryStage;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class DisciplinaryCaseSummary {
    private Long id;
    private String reason;
    private DisciplinaryStage currentStage;
    private boolean closed;
    private LocalDateTime openedAt;
    private String openedByUsername;
    private EmployeeSummary employee;

    public static DisciplinaryCaseSummary from(com.wethinkcode.hrsystem.model.DisciplinaryCase c) {
        return new DisciplinaryCaseSummary(
                c.getId(),
                c.getReason(),
                c.getCurrentStage(),
                c.isClosed(),
                c.getOpenedAt(),
                c.getOpenedBy() != null ? c.getOpenedBy().getUsername() : null,
                EmployeeSummary.from(c.getEmployee())
        );
    }
}