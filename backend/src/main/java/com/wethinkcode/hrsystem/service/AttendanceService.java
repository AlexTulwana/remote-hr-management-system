package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    public AttendanceService(AttendanceRepository attendanceRepository, EmployeeRepository employeeRepository) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    public Attendance clockIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(LocalDate.now());
        attendance.setClockIn(LocalTime.now());
        return attendanceRepository.save(attendance);
    }

    public Attendance clockOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        LocalTime clockOutTime = LocalTime.now();
        attendance.setClockOut(clockOutTime);

        Duration duration = Duration.between(attendance.getClockIn(), clockOutTime);
        double hours = duration.toMinutes() / 60.0;
        attendance.setHoursWorked(Math.round(hours * 100.0) / 100.0);

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getByEmployee(Long employeeId) {
        return attendanceRepository.findByEmployeeId(employeeId);
    }

    public List<Attendance> getByBranch(Long branchId) {
        return attendanceRepository.findByEmployeeBranchId(branchId);
    }

    public List<Attendance> getAll() {
        return attendanceRepository.findAll();
    }
}
