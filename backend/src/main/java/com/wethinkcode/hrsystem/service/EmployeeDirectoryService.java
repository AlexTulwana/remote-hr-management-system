package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeDirectoryEntry;
import com.wethinkcode.hrsystem.dto.OrgChartNode;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.EmployeeSpecifications;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmployeeDirectoryService {

    private final EmployeeRepository employeeRepository;
    private final CurrentUserService currentUserService;

    public EmployeeDirectoryService(EmployeeRepository employeeRepository,
                                    CurrentUserService currentUserService) {
        this.employeeRepository = employeeRepository;
        this.currentUserService = currentUserService;
    }

    public List<EmployeeDirectoryEntry> getDirectory(Long branchId, String department, String employmentStatus) {
        Long effectiveBranchId = branchId;
        if (!currentUserService.isHrOrAdmin()) {
            effectiveBranchId = currentUserService.getCurrentBranchId();
            if (effectiveBranchId == null) {
                return List.of();
            }
        }
        List<Employee> employees = employeeRepository.findAll(
                EmployeeSpecifications.withFilters(effectiveBranchId, department, employmentStatus));
        return employees.stream()
                .map(EmployeeDirectoryEntry::from)
                .toList();
    }

    public List<OrgChartNode> getOrgChart() {
        if (!currentUserService.isHrOrAdmin()) {
            throw new AccessDeniedException("Only HR and Admin can view the full org chart");
        }
        List<Employee> roots = employeeRepository.findByReportsToIsNull();
        return roots.stream()
                .map(this::buildNode)
                .toList();
    }

    public OrgChartNode getOrgChartFrom(Long employeeId) {
        Employee root = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        return buildNode(root);
    }

    private OrgChartNode buildNode(Employee employee) {
        List<Employee> reports = employeeRepository.findByReportsToId(employee.getId());
        List<OrgChartNode> childNodes = reports.stream()
                .map(this::buildNode)
                .toList();

        return new OrgChartNode(
                employee.getId(),
                employee.getFullName(),
                employee.getPosition(),
                employee.getDepartment(),
                employee.getBranch() != null ? employee.getBranch().getName() : null,
                childNodes
        );
    }
}