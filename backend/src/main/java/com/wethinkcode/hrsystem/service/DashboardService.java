package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.DashboardStats;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.AttendanceRepository;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.LeaveRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
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
    private final CurrentUserService currentUserService;

    public DashboardService(EmployeeRepository employeeRepository,
                            BranchRepository branchRepository,
                            LeaveRequestRepository leaveRequestRepository,
                            AttendanceRepository attendanceRepository,
                            CurrentUserService currentUserService) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository = attendanceRepository;
        this.currentUserService = currentUserService;
    }

    public DashboardStats getStats() {
        User current = currentUserService.getCurrentUser();
        String role = current.getRole();

        if ("HR".equals(role) || "ADMIN".equals(role)) {
            return getFullStats();
        }
        if ("MANAGER".equals(role)) {
            Employee managerEmployee = current.getEmployee();
            if (managerEmployee == null || managerEmployee.getBranch() == null) {
                throw new AccessDeniedException("Manager has no associated branch");
            }
            return getBranchStats(managerEmployee.getBranch());
        }
        throw new AccessDeniedException("Not authorized to view dashboard stats");
    }

    private DashboardStats getFullStats() {
        List<Employee> employees = employeeRepository.findAll();
        long totalEmployees = employees.size();

        Map<String, Long> employeesPerBranch = employees.stream()
                .filter(e -> e.getBranch() != null)
                .collect(Collectors.groupingBy(e -> e.getBranch().getName(), Collectors.counting()));

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

    private DashboardStats getBranchStats(Branch branch) {
        List<Employee> branchEmployees = employeeRepository.findByBranchId(branch.getId());
        long totalEmployees = branchEmployees.size();

        Map<String, Long> employeesPerBranch = Map.of(branch.getName(), totalEmployees);

        long pending = leaveRequestRepository.findByStatusAndEmployeeBranchId("PENDING", branch.getId()).size();
        long approved = leaveRequestRepository.findByStatusAndEmployeeBranchId("APPROVED", branch.getId()).size();
        long rejected = leaveRequestRepository.findByStatusAndEmployeeBranchId("REJECTED", branch.getId()).size();

        long attendanceToday = attendanceRepository.findByEmployeeBranchId(branch.getId()).stream()
                .filter(a -> LocalDate.now().equals(a.getDate()))
                .count();

        return new DashboardStats(totalEmployees, employeesPerBranch, pending, approved, rejected, attendanceToday);
    }
}