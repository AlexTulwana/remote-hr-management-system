package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             EmployeeRepository employeeRepository,
                             CurrentUserService currentUserService) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    public Attendance clockIn(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        requireSelfOrManagerSameBranchOrHrAdmin(employee);

        Attendance attendance = new Attendance();
        attendance.setEmployee(employee);
        attendance.setDate(LocalDate.now());
        attendance.setClockIn(LocalTime.now());
        return attendanceRepository.save(attendance);
    }

    public Attendance clockOut(Long attendanceId) {
        Attendance attendance = attendanceRepository.findById(attendanceId)
                .orElseThrow(() -> new RuntimeException("Attendance record not found"));

        requireSelfOrManagerSameBranchOrHrAdmin(attendance.getEmployee());

        LocalTime clockOutTime = LocalTime.now();
        attendance.setClockOut(clockOutTime);

        Duration duration = Duration.between(attendance.getClockIn(), clockOutTime);
        double hours = duration.toMinutes() / 60.0;
        attendance.setHoursWorked(Math.round(hours * 100.0) / 100.0);

        return attendanceRepository.save(attendance);
    }

    public List<Attendance> getByEmployee(Long employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        requireSelfOrManagerSameBranchOrHrAdmin(employee);

        return attendanceRepository.findByEmployeeId(employeeId);
    }

    public List<Attendance> getByBranch(Long branchId) {
        requireManagerOwnBranchOrHrAdmin(branchId);
        return attendanceRepository.findByEmployeeBranchId(branchId);
    }

    public List<Attendance> getAll() {
        // HR/ADMIN only - enforced via @PreAuthorize on the controller
        return attendanceRepository.findAll();
    }

    private void requireSelfOrManagerSameBranchOrHrAdmin(Employee targetEmployee) {
        User current = currentUserService.getCurrentUser();
        String role = current.getRole();

        if ("HR".equals(role) || "ADMIN".equals(role)) {
            return;
        }
        if (current.getEmployee() != null && current.getEmployee().getId().equals(targetEmployee.getId())) {
            return;
        }
        if ("MANAGER".equals(role) && sameBranch(current.getEmployee(), targetEmployee)) {
            return;
        }
        throw new AccessDeniedException("Not authorized to act on this employee's attendance");
    }

    private void requireManagerOwnBranchOrHrAdmin(Long branchId) {
        User current = currentUserService.getCurrentUser();
        String role = current.getRole();

        if ("HR".equals(role) || "ADMIN".equals(role)) {
            return;
        }
        Employee currentEmployee = current.getEmployee();
        if ("MANAGER".equals(role) && currentEmployee != null
                && currentEmployee.getBranch() != null
                && currentEmployee.getBranch().getId().equals(branchId)) {
            return;
        }
        throw new AccessDeniedException("Not authorized to view this branch's attendance");
    }

    private boolean sameBranch(Employee a, Employee b) {
        return a != null && b != null
                && a.getBranch() != null && b.getBranch() != null
                && a.getBranch().getId().equals(b.getBranch().getId());
    }
}