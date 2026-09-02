package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AttendanceService attendanceService;

    private Branch capeTown;
    private Branch testBranch;
    private Employee emma; // self, branch 1 - kept as "employee" role from original tests
    private Employee john; // not self, branch 1
    private User empUser;  // Emma logged in as EMPLOYEE
    private User mgrSameBranchUser; // manager, branch 1
    private User mgrOtherBranchUser; // manager, branch 2
    private User hrUser;
    private User adminUser;

    @BeforeEach
    void setUp() {
        capeTown = new Branch();
        capeTown.setId(1L);

        testBranch = new Branch();
        testBranch.setId(2L);

        emma = new Employee();
        emma.setId(4L);
        emma.setFullName("Emma Employee");
        emma.setBranch(capeTown);

        john = new Employee();
        john.setId(1L);
        john.setBranch(capeTown);

        empUser = new User();
        empUser.setRole("EMPLOYEE");
        empUser.setEmployee(emma);

        Employee sam = new Employee();
        sam.setId(3L);
        sam.setBranch(capeTown);
        mgrSameBranchUser = new User();
        mgrSameBranchUser.setRole("MANAGER");
        mgrSameBranchUser.setEmployee(sam);

        Employee otherBranchManager = new Employee();
        otherBranchManager.setId(9L);
        otherBranchManager.setBranch(testBranch);
        mgrOtherBranchUser = new User();
        mgrOtherBranchUser.setRole("MANAGER");
        mgrOtherBranchUser.setEmployee(otherBranchManager);

        hrUser = new User();
        hrUser.setRole("HR");

        adminUser = new User();
        adminUser.setRole("ADMIN");
    }

    // ---- clockIn: original behavior ----

    @Test
    void clockIn_validEmployee_createsAttendanceRecord() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(emma));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockIn(4L);

        assertThat(result.getEmployee()).isEqualTo(emma);
        assertThat(result.getDate()).isEqualTo(java.time.LocalDate.now());
        assertThat(result.getClockIn()).isNotNull();
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void clockIn_unknownEmployee_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockIn(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Employee not found");
    }

    // ---- clockIn: access control ----

    @Test
    void clockIn_notSelfNotManagerNotHrAdmin_throwsAccessDenied() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(john));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);

        assertThatThrownBy(() -> attendanceService.clockIn(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clockIn_managerSameBranch_succeeds() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(emma));
        when(currentUserService.getCurrentUser()).thenReturn(mgrSameBranchUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockIn(4L);

        assertThat(result.getEmployee()).isEqualTo(emma);
    }

    @Test
    void clockIn_managerDifferentBranch_throwsAccessDenied() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(emma));
        when(currentUserService.getCurrentUser()).thenReturn(mgrOtherBranchUser);

        assertThatThrownBy(() -> attendanceService.clockIn(4L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void clockIn_hr_succeedsRegardlessOfBranch() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(john));
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockIn(1L);

        assertThat(result.getEmployee()).isEqualTo(john);
    }

    @Test
    void clockIn_admin_succeedsRegardlessOfBranch() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(john));
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockIn(1L);

        assertThat(result.getEmployee()).isEqualTo(john);
    }

    // ---- clockOut: original behavior ----

    @Test
    void clockOut_calculatesHoursWorkedCorrectly() {
        Attendance attendance = new Attendance();
        attendance.setId(1L);
        attendance.setEmployee(emma);
        attendance.setClockIn(LocalTime.now().minusHours(2));

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockOut(1L);

        assertThat(result.getClockOut()).isNotNull();
        assertThat(result.getHoursWorked()).isCloseTo(2.0, within(0.02));
    }

    @Test
    void clockOut_unknownRecord_throwsException() {
        when(attendanceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> attendanceService.clockOut(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Attendance record not found");
    }

    // ---- clockOut: access control ----

    @Test
    void clockOut_managerSameBranch_succeeds() {
        Attendance attendance = new Attendance();
        attendance.setId(2L);
        attendance.setEmployee(emma);
        attendance.setClockIn(LocalTime.of(9, 0));

        when(attendanceRepository.findById(2L)).thenReturn(Optional.of(attendance));
        when(currentUserService.getCurrentUser()).thenReturn(mgrSameBranchUser);
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockOut(2L);

        assertThat(result.getClockOut()).isNotNull();
    }

    @Test
    void clockOut_notAuthorized_throwsAccessDenied() {
        Attendance attendance = new Attendance();
        attendance.setId(1L);
        attendance.setEmployee(john);
        attendance.setClockIn(LocalTime.of(9, 0));

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);

        assertThatThrownBy(() -> attendanceService.clockOut(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- getByEmployee ----

    @Test
    void getByEmployee_self_returnsRecordsForThatEmployee() {
        Attendance record = new Attendance();
        record.setId(1L);
        record.setEmployee(emma);

        when(employeeRepository.findById(4L)).thenReturn(Optional.of(emma));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);
        when(attendanceRepository.findByEmployeeId(4L)).thenReturn(List.of(record));

        List<Attendance> result = attendanceService.getByEmployee(4L);

        assertThat(result).hasSize(1).containsExactly(record);
    }

    @Test
    void getByEmployee_managerSameBranch_succeeds() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(emma));
        when(currentUserService.getCurrentUser()).thenReturn(mgrSameBranchUser);
        when(attendanceRepository.findByEmployeeId(4L)).thenReturn(List.of(new Attendance()));

        List<Attendance> result = attendanceService.getByEmployee(4L);

        assertThat(result).hasSize(1);
    }

    @Test
    void getByEmployee_notSelfNotManagerNotHrAdmin_throwsAccessDenied() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(john));
        when(currentUserService.getCurrentUser()).thenReturn(empUser);

        assertThatThrownBy(() -> attendanceService.getByEmployee(1L))
                .isInstanceOf(AccessDeniedException.class);
    }

    // ---- getByBranch ----

    @Test
    void getByBranch_managerOwnBranch_returnsRecordsForThatBranch() {
        Attendance record = new Attendance();
        record.setId(1L);

        when(currentUserService.getCurrentUser()).thenReturn(mgrSameBranchUser);
        when(attendanceRepository.findByEmployeeBranchId(1L)).thenReturn(List.of(record));

        List<Attendance> result = attendanceService.getByBranch(1L);

        assertThat(result).hasSize(1).containsExactly(record);
    }

    @Test
    void getByBranch_managerOtherBranch_throwsAccessDenied() {
        when(currentUserService.getCurrentUser()).thenReturn(mgrSameBranchUser);

        assertThatThrownBy(() -> attendanceService.getByBranch(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void getByBranch_hr_succeedsForAnyBranch() {
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(attendanceRepository.findByEmployeeBranchId(2L)).thenReturn(List.of());

        List<Attendance> result = attendanceService.getByBranch(2L);

        assertThat(result).isEmpty();
    }

    // ---- getAll: original behavior, unchanged (no auth check in service - enforced at controller) ----

    @Test
    void getAll_returnsAllRecords() {
        Attendance record1 = new Attendance();
        record1.setId(1L);
        Attendance record2 = new Attendance();
        record2.setId(2L);

        when(attendanceRepository.findAll()).thenReturn(List.of(record1, record2));

        List<Attendance> result = attendanceService.getAll();

        assertThat(result).hasSize(2);
    }
}