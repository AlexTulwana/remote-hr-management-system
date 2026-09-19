package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDirectoryEntry;
import com.wethinkcode.hrsystem.dto.OrgChartNode;
import com.wethinkcode.hrsystem.service.EmployeeDirectoryService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employee-directory")
@PreAuthorize("hasAnyRole('MANAGER','HR','ADMIN')")
public class EmployeeDirectoryController {

    private final EmployeeDirectoryService employeeDirectoryService;

    public EmployeeDirectoryController(EmployeeDirectoryService employeeDirectoryService) {
        this.employeeDirectoryService = employeeDirectoryService;
    }

    @GetMapping
    public ResponseEntity<List<EmployeeDirectoryEntry>> getDirectory(
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String department,
            @RequestParam(required = false) String employmentStatus) {
        return ResponseEntity.ok(employeeDirectoryService.getDirectory(branchId, department, employmentStatus));
    }

    @GetMapping("/org-chart")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<OrgChartNode>> getOrgChart() {
        return ResponseEntity.ok(employeeDirectoryService.getOrgChart());
    }

    @GetMapping("/org-chart/{employeeId}")
    public ResponseEntity<OrgChartNode> getOrgChartFrom(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeDirectoryService.getOrgChartFrom(employeeId));
    }
}