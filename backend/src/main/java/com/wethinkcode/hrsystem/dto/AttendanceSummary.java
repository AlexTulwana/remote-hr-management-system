package com.wethinkcode.hrsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@AllArgsConstructor
public class AttendanceSummary {
    private Long id;
    private LocalDate date;
    private LocalTime clockIn;
    private LocalTime clockOut;
    private Double hoursWorked;
    private EmployeeSummary employee;

    public static AttendanceSummary from(com.wethinkcode.hrsystem.model.Attendance a) {
        return new AttendanceSummary(
                a.getId(),
                a.getDate(),
                a.getClockIn(),
                a.getClockOut(),
                a.getHoursWorked(),
                a.getEmployee() != null ? EmployeeSummary.from(a.getEmployee()) : null
        );
    }
}
