package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.CalendarItem;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class CalendarService {

    private final CalendarEventRepository calendarEventRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final InterviewRepository interviewRepository;
    private final HearingRepository hearingRepository;
    private final JobPostingRepository jobPostingRepository;

    public CalendarService(CalendarEventRepository calendarEventRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            InterviewRepository interviewRepository,
                            HearingRepository hearingRepository,
                            JobPostingRepository jobPostingRepository) {
        this.calendarEventRepository = calendarEventRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.interviewRepository = interviewRepository;
        this.hearingRepository = hearingRepository;
        this.jobPostingRepository = jobPostingRepository;
    }

    public List<CalendarItem> getCalendar(LocalDate from, LocalDate to, Long branchId, User currentUser) {
        boolean isHrOrAdmin = "HR".equals(currentUser.getRole()) || "ADMIN".equals(currentUser.getRole());
        boolean isManager = "MANAGER".equals(currentUser.getRole());

        Long userBranchId = currentUser.getEmployee() != null && currentUser.getEmployee().getBranch() != null
                ? currentUser.getEmployee().getBranch().getId() : null;
        Long currentEmployeeId = currentUser.getEmployee() != null ? currentUser.getEmployee().getId() : null;

        List<CalendarItem> items = new ArrayList<>();

        // Native calendar events (company-wide + branch-specific), including recurrence expansion
        for (CalendarEvent event : calendarEventRepository.findAll()) {
            Long eventBranchId = event.getBranch() != null ? event.getBranch().getId() : null;

            if (branchId != null && eventBranchId != null && !eventBranchId.equals(branchId)) {
                continue;
            }
            // Non-HR/Admin only see company-wide events + their own branch's events
            if (!isHrOrAdmin && eventBranchId != null
                    && (userBranchId == null || !eventBranchId.equals(userBranchId))) {
                continue;
            }
            items.addAll(expandCalendarEvent(event, from, to));
        }

        // Leave requests (approved only)
        for (LeaveRequest leave : leaveRequestRepository.findAll()) {
            if (!"APPROVED".equals(leave.getStatus())) continue;
            if (!overlaps(leave.getStartDate(), leave.getEndDate(), from, to)) continue;

            Long leaveBranchId = leave.getEmployee().getBranch() != null
                    ? leave.getEmployee().getBranch().getId() : null;
            Long leaveEmployeeId = leave.getEmployee().getId();

            if (isHrOrAdmin) {
                // full visibility
            } else if (isManager) {
                if (userBranchId == null || !userBranchId.equals(leaveBranchId)) continue;
            } else {
                // Employee: only their own leave
                if (currentEmployeeId == null || !currentEmployeeId.equals(leaveEmployeeId)) continue;
            }

            items.add(new CalendarItem(
                    "LEAVE", leave.getId(),
                    leave.getEmployee().getFullName() + " - " + leave.getLeaveType(),
                    leave.getReason(),
                    leave.getStartDate(), leave.getEndDate(),
                    leaveBranchId
            ));
        }

        // Interviews - HR/Admin only
        if (isHrOrAdmin) {
            for (Interview interview : interviewRepository.findAll()) {
                LocalDate date = interview.getInterviewDateTime().toLocalDate();
                if (!inRange(date, from, to)) continue;
                items.add(new CalendarItem(
                        "INTERVIEW", interview.getId(),
                        "Interview: " + interview.getApplication().getCandidateName(),
                        interview.getNotes(),
                        date, date, null
                ));
            }
        }

        // Hearings
        for (Hearing hearing : hearingRepository.findAll()) {
            LocalDate date = hearing.getHearingDateTime().toLocalDate();
            if (!inRange(date, from, to)) continue;

            Long hearingBranchId = hearing.getEmployee().getBranch() != null
                    ? hearing.getEmployee().getBranch().getId() : null;
            Long hearingEmployeeId = hearing.getEmployee().getId();
            Long conductedById = hearing.getConductedBy() != null ? hearing.getConductedBy().getId() : null;

            if (isHrOrAdmin) {
                // full visibility
            } else if (isManager) {
                // Only hearings this manager is directly conducting
                if (conductedById == null || !conductedById.equals(currentUser.getId())) continue;
            } else {
                // Employee: only their own hearings
                if (currentEmployeeId == null || !currentEmployeeId.equals(hearingEmployeeId)) continue;
            }

            items.add(new CalendarItem(
                    "HEARING", hearing.getId(),
                    "Hearing: " + hearing.getEmployee().getFullName() + " - " + hearing.getCaseType(),
                    hearing.getDescription(),
                    date, date,
                    hearingBranchId
            ));
        }

        // Job postings - open/close dates visible to everyone
        for (JobPosting posting : jobPostingRepository.findAll()) {
            if (inRange(posting.getStartDate(), from, to)) {
                items.add(new CalendarItem(
                        "JOB_POSTING_OPEN", posting.getId(),
                        "Job opens: " + posting.getTitle(),
                        posting.getDepartment(),
                        posting.getStartDate(), posting.getStartDate(), null
                ));
            }
            if (inRange(posting.getEndDate(), from, to)) {
                items.add(new CalendarItem(
                        "JOB_POSTING_CLOSE", posting.getId(),
                        "Job closes: " + posting.getTitle(),
                        posting.getDepartment(),
                        posting.getEndDate(), posting.getEndDate(), null
                ));
            }
        }

        return items;
    }

    private List<CalendarItem> expandCalendarEvent(CalendarEvent event, LocalDate from, LocalDate to) {
        List<CalendarItem> result = new ArrayList<>();
        LocalDate endDate = event.getEndDate() != null ? event.getEndDate() : event.getEventDate();
        Long branchId = event.getBranch() != null ? event.getBranch().getId() : null;

        if (event.getRecurrence() == RecurrenceType.NONE) {
            if (overlaps(event.getEventDate(), endDate, from, to)) {
                result.add(toItem(event, event.getEventDate(), endDate, branchId));
            }
            return result;
        }

        LocalDate cursor = event.getEventDate();
        long spanDays = java.time.temporal.ChronoUnit.DAYS.between(event.getEventDate(), endDate);

        while (cursor.plusDays(spanDays).isBefore(from)) {
            cursor = event.getRecurrence() == RecurrenceType.WEEKLY
                    ? cursor.plusWeeks(1) : cursor.plusYears(1);
        }

        while (!cursor.isAfter(to)) {
            LocalDate instanceEnd = cursor.plusDays(spanDays);
            if (overlaps(cursor, instanceEnd, from, to)) {
                result.add(toItem(event, cursor, instanceEnd, branchId));
            }
            cursor = event.getRecurrence() == RecurrenceType.WEEKLY
                    ? cursor.plusWeeks(1) : cursor.plusYears(1);
        }
        return result;
    }

    private CalendarItem toItem(CalendarEvent event, LocalDate start, LocalDate end, Long branchId) {
        return new CalendarItem(
                "CALENDAR_EVENT", event.getId(),
                event.getTitle(), event.getDescription(),
                start, end, branchId
        );
    }

    private boolean overlaps(LocalDate startA, LocalDate endA, LocalDate startB, LocalDate endB) {
        return !startA.isAfter(endB) && !endA.isBefore(startB);
    }

    private boolean inRange(LocalDate date, LocalDate from, LocalDate to) {
        return !date.isBefore(from) && !date.isAfter(to);
    }
}
