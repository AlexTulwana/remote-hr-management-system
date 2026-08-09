package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.CalendarItem;
import com.wethinkcode.hrsystem.model.CalendarEvent;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.CalendarEventRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.service.CalendarService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;
    private final CalendarEventRepository calendarEventRepository;
    private final UserRepository userRepository;

    public CalendarController(CalendarService calendarService,
                               CalendarEventRepository calendarEventRepository,
                               UserRepository userRepository) {
        this.calendarService = calendarService;
        this.calendarEventRepository = calendarEventRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<CalendarItem>> getCalendar(
            @RequestParam LocalDate from,
            @RequestParam LocalDate to,
            @RequestParam(required = false) Long branchId) {
        return ResponseEntity.ok(calendarService.getCalendar(from, to, branchId));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PostMapping("/events")
    public ResponseEntity<CalendarEvent> createEvent(@RequestBody CalendarEvent event,
                                                       Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Managers can only create events for their own branch (or their own branch is required if set)
        if ("MANAGER".equals(currentUser.getRole())) {
            if (currentUser.getEmployee() == null || currentUser.getEmployee().getBranch() == null) {
                throw new RuntimeException("Manager has no associated branch");
            }
            event.setBranch(currentUser.getEmployee().getBranch());
        }

        event.setCreatedBy(currentUser);
        CalendarEvent saved = calendarEventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @PutMapping("/events/{id}")
    public ResponseEntity<CalendarEvent> updateEvent(@PathVariable Long id,
                                                       @RequestBody CalendarEvent updated,
                                                       Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CalendarEvent existing = calendarEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Calendar event not found"));

        if ("MANAGER".equals(currentUser.getRole())) {
            Long managerBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                    ? currentUser.getEmployee().getBranch().getId() : null;
            if (managerBranchId == null || existing.getBranch() == null
                    || !existing.getBranch().getId().equals(managerBranchId)) {
                throw new RuntimeException("Managers can only edit events for their own branch");
            }
        }

        existing.setTitle(updated.getTitle());
        existing.setDescription(updated.getDescription());
        existing.setEventDate(updated.getEventDate());
        existing.setEndDate(updated.getEndDate());
        existing.setEventType(updated.getEventType());
        existing.setRecurrence(updated.getRecurrence());
        if (!"MANAGER".equals(currentUser.getRole())) {
            existing.setBranch(updated.getBranch());
        }

        return ResponseEntity.ok(calendarEventRepository.save(existing));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN') or hasRole('MANAGER')")
    @DeleteMapping("/events/{id}")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        CalendarEvent existing = calendarEventRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Calendar event not found"));

        if ("MANAGER".equals(currentUser.getRole())) {
            Long managerBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                    ? currentUser.getEmployee().getBranch().getId() : null;
            if (managerBranchId == null || existing.getBranch() == null
                    || !existing.getBranch().getId().equals(managerBranchId)) {
                throw new RuntimeException("Managers can only delete events for their own branch");
            }
        }

        calendarEventRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }
}
