package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.ManagerHeadcountSummary;
import com.wethinkcode.hrsystem.dto.ManagerScheduleItem;
import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import com.wethinkcode.hrsystem.model.EmployeeRequest;
import com.wethinkcode.hrsystem.model.LeaveRequest;
import com.wethinkcode.hrsystem.repository.EmployeeRequestRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.service.ManagerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.wethinkcode.hrsystem.dto.DisciplinaryCaseSummary;
import com.wethinkcode.hrsystem.dto.EmployeeRequestSummary;
import com.wethinkcode.hrsystem.dto.LeaveRequestSummary;



import java.util.List;

@RestController
@RequestMapping("/api/dashboard/manager")
public class ManagerDashboardController {

    private final EmployeeRequestRepository employeeRequestRepository;
    private final CurrentUserService currentUserService;
    private final ManagerDashboardService managerDashboardService;

    public ManagerDashboardController(EmployeeRequestRepository employeeRequestRepository,
                                      CurrentUserService currentUserService,
                                      ManagerDashboardService managerDashboardService) {
        this.employeeRequestRepository = employeeRequestRepository;
        this.currentUserService = currentUserService;
        this.managerDashboardService = managerDashboardService;
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/pending-requests")
    public ResponseEntity<List<EmployeeRequestSummary>> getPendingRequests() {
        Long branchId = currentUserService.getCurrentUser().getEmployee().getBranch().getId();
        List<EmployeeRequestSummary> summaries = employeeRequestRepository
                .findByStatusAndEmployeeBranchId("PENDING", branchId).stream()
                .map(EmployeeRequestSummary::from)
                .toList();
        return ResponseEntity.ok(summaries);
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/headcount")
    public ResponseEntity<ManagerHeadcountSummary> getHeadcount() {
        Long branchId = currentUserService.getCurrentUser().getEmployee().getBranch().getId();
        return ResponseEntity.ok(managerDashboardService.getHeadcount(branchId));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/disciplinary-cases")
    public ResponseEntity<List<DisciplinaryCaseSummary>> getDisciplinaryCases() {
        Long branchId = currentUserService.getCurrentUser().getEmployee().getBranch().getId();
        return ResponseEntity.ok(managerDashboardService.getOpenDisciplinaryCases(branchId));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/leave")
    public ResponseEntity<List<LeaveRequestSummary>> getPendingLeave() {
        Long branchId = currentUserService.getCurrentUser().getEmployee().getBranch().getId();
        return ResponseEntity.ok(managerDashboardService.getPendingLeave(branchId));
    }

    @PreAuthorize("hasRole('MANAGER')")
    @GetMapping("/schedule")
    public ResponseEntity<List<ManagerScheduleItem>> getUpcomingSchedule() {
        Long branchId = currentUserService.getCurrentUser().getEmployee().getBranch().getId();
        return ResponseEntity.ok(managerDashboardService.getUpcomingSchedule(branchId));
    }
}