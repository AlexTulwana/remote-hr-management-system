package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private Employee employee;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(4L);
        employee.setFullName("Emma Employee");
    }

    // --- clockIn ---

    @Test
    void clockIn_validEmployee_createsAttendanceRecord() {
        when(employeeRepository.findById(4L)).thenReturn(Optional.of(employee));
        when(attendanceRepository.save(any(Attendance.class))).thenAnswer(inv -> inv.getArgument(0));

        Attendance result = attendanceService.clockIn(4L);

        assertThat(result.getEmployee()).isEqualTo(employee);
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

    // --- clockOut ---

    @Test
    void clockOut_calculatesHoursWorkedCorrectly() {
        Attendance attendance = new Attendance();
        attendance.setId(1L);
        attendance.setEmployee(employee);
        attendance.setClockIn(LocalTime.now().minusHours(2));

        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(attendance));
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

    // --- getByEmployee / getByBranch / getAll ---

    @Test
    void getByEmployee_returnsRecordsForThatEmployee() {
        Attendance record = new Attendance();
        record.setId(1L);
        record.setEmployee(employee);

        when(attendanceRepository.findByEmployeeId(4L)).thenReturn(List.of(record));

        List<Attendance> result = attendanceService.getByEmployee(4L);

        assertThat(result).hasSize(1).containsExactly(record);
    }

    @Test
    void getByBranch_returnsRecordsForThatBranch() {
        Attendance record = new Attendance();
        record.setId(1L);

        when(attendanceRepository.findByEmployeeBranchId(1L)).thenReturn(List.of(record));

        List<Attendance> result = attendanceService.getByBranch(1L);

        assertThat(result).hasSize(1).containsExactly(record);
    }

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