package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeDashboardSummary;
import com.wethinkcode.hrsystem.model.Attendance;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.DisciplinaryCaseRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class EmployeeDashboardService {

    private final CurrentUserService currentUserService;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRequestRepository employeeRequestRepository;
    private final DisciplinaryCaseRepository disciplinaryCaseRepository;

    public EmployeeDashboardService(CurrentUserService currentUserService,
                                    LeaveRequestRepository leaveRequestRepository,
                                    AttendanceRepository attendanceRepository,
                                    EmployeeRequestRepository employeeRequestRepository,
                                    DisciplinaryCaseRepository disciplinaryCaseRepository) {
        this.currentUserService = currentUserService;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.employeeRequestRepository = employeeRequestRepository;
        this.disciplinaryCaseRepository = disciplinaryCaseRepository;
    }

    public EmployeeDashboardSummary getSummary() {
        Employee employee = currentUserService.getCurrentUser().getEmployee();

        List<LeaveRequest> leave = leaveRequestRepository.findByEmployeeId(employee.getId());
        long pendingLeave = leave.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long approvedLeave = leave.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        long rejectedLeave = leave.stream().filter(l -> "REJECTED".equals(l.getStatus())).count();

        List<Attendance> attendance = attendanceRepository.findByEmployeeId(employee.getId());
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        long attendanceThisMonth = attendance.stream()
                .filter(a -> a.getDate() != null && !a.getDate().isBefore(monthStart))
                .count();

        List<EmployeeRequest> requests = employeeRequestRepository.findByEmployeeId(employee.getId());
        long pendingRequests = requests.stream().filter(r -> "PENDING".equals(r.getStatus())).count();

        List<DisciplinaryCase> cases = disciplinaryCaseRepository.findByEmployeeId(employee.getId());
        long openCases = cases.stream().filter(c -> !c.isClosed()).count();

        return new EmployeeDashboardSummary(
                employee.getEmployeeNumber(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getDepartment(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                employee.getEmploymentStatus(),
                pendingLeave,
                approvedLeave,
                rejectedLeave,
                attendanceThisMonth,
                pendingRequests,
                openCases
        );
    }
}