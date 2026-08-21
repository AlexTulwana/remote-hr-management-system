package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@Data
@AllArgsConstructor
public class TurnoverSummary {
    private LocalDate asOfDate;
    private double turnoverRate30d;
}