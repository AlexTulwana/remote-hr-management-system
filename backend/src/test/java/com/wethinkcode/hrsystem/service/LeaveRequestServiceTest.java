package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.LeaveRequestDto;
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

import java.time.LocalDate;
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
}