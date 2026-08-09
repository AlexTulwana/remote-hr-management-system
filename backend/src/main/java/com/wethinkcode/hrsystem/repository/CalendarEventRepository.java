package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.CalendarEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface CalendarEventRepository extends JpaRepository<CalendarEvent, Long> {

    List<CalendarEvent> findByBranchIsNullAndEventDateLessThanEqualAndEndDateGreaterThanEqual(
            LocalDate rangeEnd, LocalDate rangeStart);

    List<CalendarEvent> findByBranchIdAndEventDateLessThanEqualAndEndDateGreaterThanEqual(
            Long branchId, LocalDate rangeEnd, LocalDate rangeStart);

    List<CalendarEvent> findByRecurrenceNot(com.wethinkcode.hrsystem.model.RecurrenceType none);
}
