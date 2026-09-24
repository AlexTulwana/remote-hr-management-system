package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import com.wethinkcode.hrsystem.dto.LeaveBalanceResponse;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LeaveRequestServiceTest {

    @Mock
    private LeaveRequestRepository leaveRequestRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private LeaveRequestService leaveRequestService;

    private Employee employee;
    private LeaveRequest existingLeave;

    @TempDir
    Path tempDir;

    private static final List<String> COMMITTED = List.of("PENDING", "APPROVED");

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");

        existingLeave = new LeaveRequest();
        existingLeave.setId(1L);
        existingLeave.setEmployee(employee);
        existingLeave.setStatus("PENDING");
    }

    // --- submit ---

    @Test
    void submit_validRequest_savesLeaveRequest() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType("ANNUAL");
        dto.setStartDate(LocalDate.of(2026, 9, 1));
        dto.setEndDate(LocalDate.of(2026, 9, 5));
        dto.setReason("Family vacation");

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.submit(4L, dto, null);

        assertThat(result.getEmployee()).isEqualTo(employee);
        assertThat(result.getLeaveType()).isEqualTo("ANNUAL");
        assertThat(result.getStatus()).isEqualTo("PENDING");
        assertThat(result.getAttachmentPath()).isNull();
    }

    @Test
    void submit_unknownEmployee_throwsException() {
        LeaveRequestDto dto = new LeaveRequestDto();

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.submit(99L, dto, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
    }

    @Test
    void submit_withAttachment_savesAttachmentPath() {
        LeaveRequestDto dto = new LeaveRequestDto();
        dto.setLeaveType("SICK");
        dto.setStartDate(LocalDate.of(2026, 9, 1));
        dto.setEndDate(LocalDate.of(2026, 9, 2));
        dto.setReason("Flu");

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        MockMultipartFile file = new MockMultipartFile("attachment", "note.pdf", "application/pdf", "content".getBytes());

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.submit(4L, dto, file);

        assertThat(result.getAttachmentPath()).isNotNull();
    }

    // --- approve / reject ---

    @Test
    void approve_setsStatusApproved() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.approve(1L);

        assertThat(result.getStatus()).isEqualTo("APPROVED");
    }

    @Test
    void reject_setsStatusRejectedWithReason() {
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.reject(1L, "Insufficient leave balance");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getRejectionReason()).isEqualTo("Insufficient leave balance");
    }

    @Test
    void approve_alreadyApproved_throwsIllegalState() {
        existingLeave.setStatus("APPROVED");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        assertThatThrownBy(() -> leaveRequestService.approve(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void reject_alreadyRejected_throwsIllegalState() {
        existingLeave.setStatus("REJECTED");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        assertThatThrownBy(() -> leaveRequestService.reject(1L, "reason"))
                .isInstanceOf(IllegalStateException.class);
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void approve_alreadyRejected_throwsIllegalState() {
        existingLeave.setStatus("REJECTED");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        assertThatThrownBy(() -> leaveRequestService.approve(1L))
                .isInstanceOf(IllegalStateException.class);
        verify(leaveRequestRepository, never()).save(any());
    }

    // --- getById ---

    @Test
    void getById_notFound_throwsException() {
        when(leaveRequestRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.getById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Leave request not found");
    }

    // --- getByEmployee (authorization logic) ---

    @Test
    void getByEmployee_asHr_bypassesSelfCheck() {
        User hrUser = new User();
        hrUser.setRole("HR");

        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(leaveRequestRepository.findByEmployeeId(4L)).thenReturn(List.of(existingLeave));

        List<LeaveRequest> result = leaveRequestService.getByEmployee(4L);

        assertThat(result).containsExactly(existingLeave);
        verify(currentUserService, never()).isSelf(anyLong());
    }

    @Test
    void getByEmployee_asSelf_isAllowed() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");

        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(4L)).thenReturn(true);
        when(leaveRequestRepository.findByEmployeeId(4L)).thenReturn(List.of(existingLeave));

        List<LeaveRequest> result = leaveRequestService.getByEmployee(4L);

        assertThat(result).containsExactly(existingLeave);
    }

    @Test
    void getByEmployee_asOtherEmployee_throwsAccessDenied() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");

        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestService.getByEmployee(4L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not authorized to view these leave requests");
    }

    // --- getAll / getByBranch ---

    @Test
    void getAll_returnsAllLeaveRequests() {
        when(leaveRequestRepository.findAll()).thenReturn(List.of(existingLeave));

        List<LeaveRequest> result = leaveRequestService.getAll();

        assertThat(result).containsExactly(existingLeave);
    }

    @Test
    void getByBranch_returnsLeaveRequestsForBranch() {
        when(leaveRequestRepository.findByEmployeeBranchId(1L)).thenReturn(List.of(existingLeave));

        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);

        List<LeaveRequest> result = leaveRequestService.getByBranch(1L);

        assertThat(result).containsExactly(existingLeave);
    }

    // ---------- helpers for leave rules ----------

    private void asHr() {
        User hr = new User();
        hr.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
    }

    private LeaveRequestDto dto(String type, LocalDate start, LocalDate end) {
        LeaveRequestDto d = new LeaveRequestDto();
        d.setLeaveType(type);
        d.setStartDate(start);
        d.setEndDate(end);
        d.setReason("Test");
        return d;
    }

    private LeaveRequest committed(String type, String status, LocalDate start, LocalDate end) {
        LeaveRequest l = new LeaveRequest();
        l.setEmployee(employee);
        l.setLeaveType(type);
        l.setStatus(status);
        l.setStartDate(start);
        l.setEndDate(end);
        return l;
    }

    private LocalDate marchMonday() {
        return LocalDate.of(LocalDate.now().getYear(), 3, 1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    // ---------- submit: date validation ----------

    @Test
    void submit_endBeforeStart_throwsIllegalArgument() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 8)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("End date cannot be before start date");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submit_missingDates_throwsIllegalArgument() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveRequestService.submit(4L, dto("ANNUAL", null, null), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Start and end date are required");
    }

    @Test
    void submit_crossesYearEnd_throwsIllegalArgument() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot cross into a new year");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submit_weekendOnly_blockedForAnnual() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 6)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The selected dates contain no working days (Mon-Fri)");
    }

    @Test
    void submit_weekendOnly_blockedForNonAnnualToo() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("SICK", LocalDate.of(2026, 9, 5), LocalDate.of(2026, 9, 6)), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The selected dates contain no working days (Mon-Fri)");
    }

    // ---------- submit: overlap ----------

    @Test
    void submit_overlapsPending_throwsIllegalState() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("SICK", "PENDING", LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 4))));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 9, 4), LocalDate.of(2026, 9, 8)), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap with an existing pending leave request");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submit_overlapsApprovedOfDifferentType_throwsIllegalState() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 11))));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("SICK", LocalDate.of(2026, 9, 9), LocalDate.of(2026, 9, 10)), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("overlap with an existing approved leave request");
    }

    @Test
    void submit_adjacentToExisting_isAllowed() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("SICK", "PENDING", LocalDate.of(2026, 9, 3), LocalDate.of(2026, 9, 4))));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 8)), null);

        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    // ---------- submit: annual allowance ----------

    @Test
    void submit_annualOverAllowance_throwsIllegalState() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        // 10 approved working days (7-18 Sep 2026), 5 remain
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 18))));

        // 5-12 Oct 2026 = 6 working days
        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Not enough annual leave for 2026: requested 6 working days but only 5 remain");
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    void submit_annualExactlyUpToAllowance_isAllowed() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 18))));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // 5-9 Oct 2026 = 5 working days, total exactly 15
        LeaveRequest result = leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 9)), null);

        assertThat(result.getStatus()).isEqualTo("PENDING");
    }

    @Test
    void submit_annualPendingCountsTowardAllowance() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "PENDING", LocalDate.of(2026, 9, 7), LocalDate.of(2026, 9, 18))));

        assertThatThrownBy(() -> leaveRequestService.submit(4L,
                dto("ANNUAL", LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 12)), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("only 5 remain");
    }

    @Test
    void submit_nonAnnualHasNoLimit() {
        asHr();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 20))));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        // whole of October, ANNUAL already exhausted, SICK is still allowed
        LeaveRequest result = leaveRequestService.submit(4L,
                dto("SICK", LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30)), null);

        assertThat(result.getLeaveType()).isEqualTo("SICK");
    }

    // ---------- decisions ----------

    @Test
    void approve_recordsDecidedByNameAndTime() {
        User hr = new User();
        hr.setRole("HR");
        Employee hrEmployee = new Employee();
        hrEmployee.setFullName("Naomi Baker");
        hr.setEmployee(hrEmployee);
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.approve(1L);

        assertThat(result.getDecidedByName()).isEqualTo("Naomi Baker");
        assertThat(result.getDecidedAt()).isNotNull();
    }

    @Test
    void reject_recordsDecisionAndFallsBackToUsername() {
        User hr = new User();
        hr.setRole("HR");
        hr.setUsername("hrtest1");
        when(currentUserService.getCurrentUser()).thenReturn(hr);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveRequest result = leaveRequestService.reject(1L, "Peak period");

        assertThat(result.getStatus()).isEqualTo("REJECTED");
        assertThat(result.getRejectionReason()).isEqualTo("Peak period");
        assertThat(result.getDecidedByName()).isEqualTo("hrtest1");
        assertThat(result.getDecidedAt()).isNotNull();
    }

    // ---------- balances ----------

    @Test
    void getBalance_splitsUsedReservedAndOtherDays() {
        asHr();
        LocalDate m = marchMonday();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", m, m.plusDays(11)),                    // 10 days used
                committed("ANNUAL", "PENDING", m.plusDays(14), m.plusDays(16)),        // 3 days reserved
                committed("SICK", "APPROVED", m.plusDays(21), m.plusDays(22)),         // 2 other days
                committed("SICK", "PENDING", m.plusDays(28), m.plusDays(29))));        // pending non-annual ignored

        LeaveBalanceResponse r = leaveRequestService.getBalance(4L);

        assertThat(r.getEmployeeId()).isEqualTo(4L);
        assertThat(r.getYear()).isEqualTo(LocalDate.now().getYear());
        assertThat(r.getAllowance()).isEqualTo(15);
        assertThat(r.getUsed()).isEqualTo(10);
        assertThat(r.getReserved()).isEqualTo(3);
        assertThat(r.getRemaining()).isEqualTo(2);
        assertThat(r.getOtherDaysTaken()).isEqualTo(2);
    }

    @Test
    void getBalance_remainingNeverGoesNegative() {
        asHr();
        LocalDate m = marchMonday();
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.findByEmployeeIdAndStatusIn(4L, COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", m, m.plusDays(25))));                  // 20 days

        LeaveBalanceResponse r = leaveRequestService.getBalance(4L);

        assertThat(r.getUsed()).isEqualTo(20);
        assertThat(r.getRemaining()).isEqualTo(0);
    }

    @Test
    void getBalance_asOtherEmployee_throwsAccessDenied() {
        User other = new User();
        other.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestService.getBalance(4L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getBalance_unknownEmployee_throwsException() {
        asHr();
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveRequestService.getBalance(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
    }

    @Test
    void getAllBalances_onlyIncludesActiveEmployees() {
        when(currentUserService.isHrOrAdmin()).thenReturn(true);
        LocalDate m = marchMonday();

        Employee resigned = new Employee();
        resigned.setId(5L);
        resigned.setEmploymentStatus("RESIGNED");
        Employee onboarding = new Employee();
        onboarding.setId(6L);
        onboarding.setEmploymentStatus("ONBOARDING");

        when(leaveRequestRepository.findByStatusIn(COMMITTED)).thenReturn(List.of(
                committed("ANNUAL", "APPROVED", m, m.plusDays(4))));                   // 5 days for employee 4
        when(employeeRepository.findAll()).thenReturn(List.of(employee, resigned, onboarding));

        List<LeaveBalanceResponse> result = leaveRequestService.getAllBalances();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getEmployeeId()).isEqualTo(4L);
        assertThat(result.get(0).getUsed()).isEqualTo(5);
        assertThat(result.get(0).getRemaining()).isEqualTo(10);
    }

    @Test
    void getAllBalances_notHrOrAdmin_throwsAccessDenied() {
        assertThatThrownBy(() -> leaveRequestService.getAllBalances())
                .isInstanceOf(AccessDeniedException.class);
        verify(employeeRepository, never()).findAll();
    }

    // ---------- attachments ----------

    @Test
    void getAttachmentPath_asOwner_returnsPath() throws Exception {
        Path file = tempDir.resolve("abc_note.pdf");
        Files.writeString(file, "content");
        existingLeave.setAttachmentPath(file.toString());
        User owner = new User();
        owner.setRole("EMPLOYEE");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(currentUserService.getCurrentUser()).thenReturn(owner);
        when(currentUserService.isSelf(4L)).thenReturn(true);

        assertThat(leaveRequestService.getAttachmentPath(1L)).isEqualTo(file);
    }

    @Test
    void getAttachmentPath_asHr_returnsPath() throws Exception {
        Path file = tempDir.resolve("abc_note.pdf");
        Files.writeString(file, "content");
        existingLeave.setAttachmentPath(file.toString());
        asHr();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        assertThat(leaveRequestService.getAttachmentPath(1L)).isEqualTo(file);
    }

    @Test
    void getAttachmentPath_asOtherEmployee_throwsAccessDenied() {
        existingLeave.setAttachmentPath("uploads/whatever.pdf");
        User other = new User();
        other.setRole("EMPLOYEE");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(currentUserService.getCurrentUser()).thenReturn(other);
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestService.getAttachmentPath(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not authorized to view this attachment");
    }

    @Test
    void getAttachmentPath_managerWithoutBranch_throwsAccessDenied() {
        existingLeave.setAttachmentPath("uploads/whatever.pdf");
        User manager = new User();
        manager.setRole("MANAGER");
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestService.getAttachmentPath(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getAttachmentPath_noAttachment_throwsException() {
        asHr();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        assertThatThrownBy(() -> leaveRequestService.getAttachmentPath(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("This leave request has no attachment");
    }

    @Test
    void getAttachmentPath_fileMissingOnDisk_throwsException() {
        existingLeave.setAttachmentPath(tempDir.resolve("gone.pdf").toString());
        asHr();
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));

        assertThatThrownBy(() -> leaveRequestService.getAttachmentPath(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Attachment file not found");
    }

    private User managerInBranch(Long branchId) {
        Branch b = new Branch();
        b.setId(branchId);
        Employee managerEmployee = new Employee();
        managerEmployee.setBranch(b);
        User manager = new User();
        manager.setRole("MANAGER");
        manager.setEmployee(managerEmployee);
        return manager;
    }

    @Test
    void getAttachmentPath_managerSameBranch_returnsPath() throws Exception {
        Path file = tempDir.resolve("abc_note.pdf");
        Files.writeString(file, "content");
        existingLeave.setAttachmentPath(file.toString());
        Branch b = new Branch();
        b.setId(1L);
        employee.setBranch(b);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(currentUserService.getCurrentUser()).thenReturn(managerInBranch(1L));
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThat(leaveRequestService.getAttachmentPath(1L)).isEqualTo(file);
    }

    @Test
    void getAttachmentPath_managerDifferentBranch_throwsAccessDenied() {
        existingLeave.setAttachmentPath("uploads/whatever.pdf");
        Branch b = new Branch();
        b.setId(1L);
        employee.setBranch(b);
        when(leaveRequestRepository.findById(1L)).thenReturn(Optional.of(existingLeave));
        when(currentUserService.getCurrentUser()).thenReturn(managerInBranch(2L));
        when(currentUserService.isSelf(4L)).thenReturn(false);

        assertThatThrownBy(() -> leaveRequestService.getAttachmentPath(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("You are not authorized to view this attachment");
    }
}
