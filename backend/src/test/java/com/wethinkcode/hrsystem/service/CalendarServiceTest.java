package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.CalendarItem;
import com.wethinkcode.hrsystem.model.*;
import com.wethinkcode.hrsystem.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CalendarServiceTest {

    @Mock private CalendarEventRepository calendarEventRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private InterviewRepository interviewRepository;
    @Mock private HearingRepository hearingRepository;
    @Mock private JobPostingRepository jobPostingRepository;
    @Mock private AnnouncementRepository announcementRepository;

    private CalendarService calendarService;

    private Branch branch1;
    private Branch branch2;
    private Employee employee1;
    private Employee employee2;
    private Employee managerEmployee;
    private User hrUser;
    private User managerUser;
    private User employeeUser;

    private final LocalDate from = LocalDate.of(2026, 1, 1);
    private final LocalDate to = LocalDate.of(2026, 1, 31);

    @BeforeEach
    void setUp() {
        calendarService = new CalendarService(calendarEventRepository, leaveRequestRepository,
                interviewRepository, hearingRepository, jobPostingRepository, announcementRepository);

        branch1 = new Branch();
        branch1.setId(1L);
        branch2 = new Branch();
        branch2.setId(2L);

        employee1 = new Employee();
        employee1.setId(1L);
        employee1.setFullName("Emma Employee");
        employee1.setBranch(branch1);

        employee2 = new Employee();
        employee2.setId(2L);
        employee2.setFullName("Other Employee");
        employee2.setBranch(branch2);

        managerEmployee = new Employee();
        managerEmployee.setId(3L);
        managerEmployee.setBranch(branch1);

        hrUser = new User();
        hrUser.setId(100L);
        hrUser.setRole("HR");

        managerUser = new User();
        managerUser.setId(200L);
        managerUser.setRole("MANAGER");
        managerUser.setEmployee(managerEmployee);

        employeeUser = new User();
        employeeUser.setId(300L);
        employeeUser.setRole("EMPLOYEE");
        employeeUser.setEmployee(employee1);

        // default empty stubs shared by every branch, overridden per-test where needed
        when(leaveRequestRepository.findAll()).thenReturn(List.of());
        when(hearingRepository.findAll()).thenReturn(List.of());
        when(jobPostingRepository.findAll()).thenReturn(List.of());
        when(announcementRepository.findAll()).thenReturn(List.of());
    }

    private CalendarEvent baseEvent(LocalDate eventDate, Branch branch, RecurrenceType recurrence) {
        CalendarEvent event = new CalendarEvent();
        event.setId(1L);
        event.setTitle("Company Meeting");
        event.setDescription("desc");
        event.setEventDate(eventDate);
        event.setBranch(branch);
        event.setRecurrence(recurrence);
        return event;
    }

    // ---------- Calendar events ----------

    @Test
    void getCalendar_hrSeesCompanyWideEvent() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 10), null, RecurrenceType.NONE)));
        when(interviewRepository.findAll()).thenReturn(List.of());

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
        assertEquals("CALENDAR_EVENT", result.get(0).getSourceType());
    }

    @Test
    void getCalendar_employeeSeesOwnBranchEvent() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 10), branch1, RecurrenceType.NONE)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(1, result.size());
    }

    @Test
    void getCalendar_employeeExcludedFromOtherBranchEvent() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 10), branch2, RecurrenceType.NONE)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_branchIdParam_excludesOtherBranchEvent() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 10), branch2, RecurrenceType.NONE)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, 1L, hrUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_eventOutOfDateRange_excluded() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 3, 1), null, RecurrenceType.NONE)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_recurrenceWeekly_expandsOccurrencesInRange() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 5), null, RecurrenceType.WEEKLY)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        // Jan 5, 12, 19, 26 all fall within Jan 1-31
        assertEquals(4, result.size());
    }

    @Test
    void getCalendar_recurrenceAnnual_onlyOneOccurrenceInOneMonthRange() {
        when(calendarEventRepository.findAll()).thenReturn(List.of(baseEvent(LocalDate.of(2026, 1, 15), null, RecurrenceType.ANNUAL)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
    }

    // ---------- Leave requests ----------

    private LeaveRequest leave(Employee employee, String status, LocalDate start, LocalDate end) {
        LeaveRequest leave = new LeaveRequest();
        leave.setEmployee(employee);
        leave.setStatus(status);
        leave.setStartDate(start);
        leave.setEndDate(end);
        leave.setLeaveType("ANNUAL");
        leave.setReason("vacation");
        return leave;
    }

    @Test
    void getCalendar_leaveRequest_pendingExcluded() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(employee1, "PENDING", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_leaveRequest_hrSeesAllBranches() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(employee2, "APPROVED", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
        assertEquals("LEAVE", result.get(0).getSourceType());
    }

    @Test
    void getCalendar_leaveRequest_managerExcludedFromOtherBranch() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(employee2, "APPROVED", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, managerUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_leaveRequest_managerSeesOwnBranch() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(employee1, "APPROVED", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, managerUser);

        assertEquals(1, result.size());
    }

    @Test
    void getCalendar_leaveRequest_employeeExcludedFromColleagueLeave() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(managerEmployee, "APPROVED", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_leaveRequest_employeeSeesOwnLeave() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(leaveRequestRepository.findAll()).thenReturn(
                List.of(leave(employee1, "APPROVED", LocalDate.of(2026, 1, 10), LocalDate.of(2026, 1, 12))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(1, result.size());
    }

    // ---------- Interviews ----------

    private Interview interview(LocalDate date) {
        Application app = new Application();
        app.setCandidateName("Jane Candidate");
        Interview interview = new Interview();
        interview.setApplication(app);
        interview.setInterviewDateTime(date.atTime(10, 0));
        interview.setNotes("first round");
        return interview;
    }

    @Test
    void getCalendar_interview_visibleToHr() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(interviewRepository.findAll()).thenReturn(List.of(interview(LocalDate.of(2026, 1, 15))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
        assertEquals("INTERVIEW", result.get(0).getSourceType());
    }

    @Test
    void getCalendar_interview_hiddenFromManager() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, managerUser);

        assertTrue(result.isEmpty());
        // interviewRepository should never even be queried for non-HR/Admin
    }

    @Test
    void getCalendar_interview_hiddenFromEmployee() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertTrue(result.isEmpty());
    }

    // ---------- Hearings ----------

    private Hearing hearing(Employee employee, User conductedBy, LocalDate date) {
        Hearing hearing = new Hearing();
        hearing.setEmployee(employee);
        hearing.setConductedBy(conductedBy);
        hearing.setCaseType("Misconduct");
        hearing.setDescription("desc");
        hearing.setHearingDateTime(date.atTime(9, 0));
        return hearing;
    }

    @Test
    void getCalendar_hearing_hrSeesAll() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(interviewRepository.findAll()).thenReturn(List.of());
        when(hearingRepository.findAll()).thenReturn(List.of(hearing(employee2, managerUser, LocalDate.of(2026, 1, 20))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
        assertEquals("HEARING", result.get(0).getSourceType());
    }

    @Test
    void getCalendar_hearing_managerSeesOnlyOwnConducted() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(hearingRepository.findAll()).thenReturn(List.of(
                hearing(employee2, managerUser, LocalDate.of(2026, 1, 20)),
                hearing(employee1, hrUser, LocalDate.of(2026, 1, 21))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, managerUser);

        assertEquals(1, result.size());
    }

    @Test
    void getCalendar_hearing_employeeSeesOwnOnly() {
        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(hearingRepository.findAll()).thenReturn(List.of(
                hearing(employee1, hrUser, LocalDate.of(2026, 1, 20)),
                hearing(employee2, hrUser, LocalDate.of(2026, 1, 21))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(1, result.size());
    }

    // ---------- Job postings ----------

    @Test
    void getCalendar_jobPosting_opensAndClosesVisibleToAll() {
        JobPosting posting = new JobPosting();
        posting.setId(1L);
        posting.setTitle("Developer Role");
        posting.setDepartment("IT");
        posting.setStartDate(LocalDate.of(2026, 1, 5));
        posting.setEndDate(LocalDate.of(2026, 1, 25));

        when(calendarEventRepository.findAll()).thenReturn(List.of());
        when(jobPostingRepository.findAll()).thenReturn(List.of(posting));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(i -> i.getSourceType().equals("JOB_POSTING_OPEN")));
        assertTrue(result.stream().anyMatch(i -> i.getSourceType().equals("JOB_POSTING_CLOSE")));
    }

    // ---------- Announcements ----------

    private Announcement announcement(LocalDate expiryDate, Branch... branches) {
        Announcement announcement = new Announcement();
        announcement.setId(1L);
        announcement.setTitle("Policy update");
        announcement.setContent("Please review the new policy.");
        announcement.setExpiryDate(expiryDate);
        if (branches.length > 0) {
            announcement.setBranches(new java.util.HashSet<>(java.util.List.of(branches)));
        }
        return announcement;
    }

    @Test
    void getCalendar_announcement_noExpiryDate_excluded() {
        when(announcementRepository.findAll()).thenReturn(List.of(announcement(null)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_announcement_everyoneVisibleToAllRoles() {
        when(announcementRepository.findAll()).thenReturn(List.of(announcement(LocalDate.of(2026, 1, 20))));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(1, result.size());
        assertEquals("ANNOUNCEMENT", result.get(0).getSourceType());
        assertEquals(LocalDate.of(2026, 1, 20), result.get(0).getDate());
    }

    @Test
    void getCalendar_announcement_branchTargeted_hiddenFromOtherBranchEmployee() {
        when(announcementRepository.findAll()).thenReturn(
                List.of(announcement(LocalDate.of(2026, 1, 20), branch2)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertTrue(result.isEmpty());
    }

    @Test
    void getCalendar_announcement_branchTargeted_visibleToOwnBranchEmployee() {
        when(announcementRepository.findAll()).thenReturn(
                List.of(announcement(LocalDate.of(2026, 1, 20), branch1)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, employeeUser);

        assertEquals(1, result.size());
    }

    @Test
    void getCalendar_announcement_hrSeesBranchTargetedRegardlessOfOwnBranch() {
        when(announcementRepository.findAll()).thenReturn(
                List.of(announcement(LocalDate.of(2026, 1, 20), branch2)));

        List<CalendarItem> result = calendarService.getCalendar(from, to, null, hrUser);

        assertEquals(1, result.size());
    }
}