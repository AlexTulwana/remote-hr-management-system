package com.wethinkcode.hrsystem.dto;

import com.wethinkcode.hrsystem.model.CalendarEvent;

import java.time.LocalDate;

public record CalendarEventResponse(
        Long id,
        String title,
        String description,
        LocalDate eventDate,
        LocalDate endDate,
        String eventType,
        String recurrence,
        Long branchId,
        String branchName
) {
    public static CalendarEventResponse from(CalendarEvent e) {
        return new CalendarEventResponse(
                e.getId(),
                e.getTitle(),
                e.getDescription(),
                e.getEventDate(),
                e.getEndDate(),
                e.getEventType() != null ? e.getEventType().name() : null,
                e.getRecurrence() != null ? e.getRecurrence().name() : null,
                e.getBranch() != null ? e.getBranch().getId() : null,
                e.getBranch() != null ? e.getBranch().getName() : null
        );
    }
}
