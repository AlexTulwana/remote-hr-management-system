package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.DashboardStats;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardService(EmployeeRepository employeeRepository,
                            BranchRepository branchRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            AttendanceRepository attendanceRepository) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public DashboardStats getStats() {
        List<Employee> employees = employeeRepository.findAll();
        long totalEmployees = employees.size();

        Map<String, Long> employeesPerBranch = employees.stream()
                .filter(e -> e.getBranch() != null)
                .collect(Collectors.groupingBy(e -> e.getBranch().getName(), Collectors.counting()));

        // Ensure branches with zero employees still show up
        List<Branch> allBranches = branchRepository.findAll();
        for (Branch branch : allBranches) {
            employeesPerBranch.putIfAbsent(branch.getName(), 0L);
        }

        List<LeaveRequest> allLeave = leaveRequestRepository.findAll();
        long pending = allLeave.stream().filter(l -> "PENDING".equals(l.getStatus())).count();
        long approved = allLeave.stream().filter(l -> "APPROVED".equals(l.getStatus())).count();
        long rejected = allLeave.stream().filter(l -> "REJECTED".equals(l.getStatus())).count();

        long attendanceToday = attendanceRepository.findAll().stream()
                .filter(a -> LocalDate.now().equals(a.getDate()))
                .count();

        return new DashboardStats(totalEmployees, employeesPerBranch, pending, approved, rejected, attendanceToday);
    }
}