package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.ManagerHeadcountSummary;
import com.wethinkcode.hrsystem.dto.ManagerScheduleItem;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.repository.CalendarEventRepository;
import com.wethinkcode.hrsystem.repository.DisciplinaryCaseRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HearingRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import com.wethinkcode.hrsystem.dto.DisciplinaryCaseSummary;
import com.wethinkcode.hrsystem.dto.LeaveRequestSummary;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ManagerDashboardService {

    private static final int SCHEDULE_WINDOW_DAYS = 14;

    private final EmployeeRepository employeeRepository;
    private final DisciplinaryCaseRepository disciplinaryCaseRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final CalendarEventRepository calendarEventRepository;
    private final HearingRepository hearingRepository;

    public ManagerDashboardService(EmployeeRepository employeeRepository,
                                   DisciplinaryCaseRepository disciplinaryCaseRepository,
                                   LeaveRequestRepository leaveRequestRepository,
                                   CalendarEventRepository calendarEventRepository,
                                   HearingRepository hearingRepository) {
        this.employeeRepository = employeeRepository;
        this.disciplinaryCaseRepository = disciplinaryCaseRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.calendarEventRepository = calendarEventRepository;
        this.hearingRepository = hearingRepository;
    }

    public ManagerHeadcountSummary getHeadcount(Long branchId) {
        List<Employee> employees = employeeRepository.findByBranchId(branchId);

        long total = employees.size();
        List<Employee> active = employees.stream()
                .filter(e -> "ACTIVE".equals(e.getEmploymentStatus()))
                .toList();

        Map<String, Long> byDepartment = active.stream()
                .collect(Collectors.groupingBy(Employee::getDepartment, Collectors.counting()));

        return new ManagerHeadcountSummary(total, active.size(), byDepartment);
    }

    public List<DisciplinaryCaseSummary> getOpenDisciplinaryCases(Long branchId) {
        return disciplinaryCaseRepository.findByEmployeeBranchIdAndClosedFalse(branchId).stream()
                .map(DisciplinaryCaseSummary::from)
                .toList();
    }

    public List<LeaveRequestSummary> getPendingLeave(Long branchId) {
        return leaveRequestRepository.findByStatusAndEmployeeBranchId("PENDING", branchId).stream()
                .map(LeaveRequestSummary::from)
                .toList();
    }

    public List<ManagerScheduleItem> getUpcomingSchedule(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate rangeEnd = today.plusDays(SCHEDULE_WINDOW_DAYS);

        List<ManagerScheduleItem> items = new ArrayList<>();

        calendarEventRepository
                .findByBranchIsNullAndEventDateLessThanEqualAndEndDateGreaterThanEqual(rangeEnd, today)
                .forEach(e -> items.add(new ManagerScheduleItem(
                        "CALENDAR_EVENT", e.getTitle(), e.getEventDate().atStartOfDay(), null)));

        calendarEventRepository
                .findByBranchIdAndEventDateLessThanEqualAndEndDateGreaterThanEqual(branchId, rangeEnd, today)
                .forEach(e -> items.add(new ManagerScheduleItem(
                        "CALENDAR_EVENT", e.getTitle(), e.getEventDate().atStartOfDay(), null)));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime windowEnd = now.plusDays(SCHEDULE_WINDOW_DAYS);

        hearingRepository
                .findByEmployeeBranchIdAndHearingDateTimeBetweenAndStatus(branchId, now, windowEnd, "SCHEDULED")
                .forEach(h -> items.add(new ManagerScheduleItem(
                        "HEARING",
                        "Disciplinary Hearing: " + h.getEmployee().getFullName(),
                        h.getHearingDateTime(),
                        h.getMeetingLink())));

        items.sort(Comparator.comparing(ManagerScheduleItem::getDateTime));
        return items;
    }
}