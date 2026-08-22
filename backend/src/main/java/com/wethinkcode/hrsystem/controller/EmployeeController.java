package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.EmployeeDetail;
import com.wethinkcode.hrsystem.dto.EmployeeRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.service.EmployeeService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Employee> create(@RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.create(request));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<List<EmployeeDetail>> getAll() {
        return ResponseEntity.ok(employeeService.getAll().stream().map(EmployeeDetail::from).toList());
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeDetail> getById(@PathVariable Long id) {
        return ResponseEntity.ok(EmployeeDetail.from(employeeService.getById(id)));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Employee> update(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        employeeService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
    @PreAuthorize("hasRole('HR') or hasRole('ADMIN')")
    @PatchMapping("/{employeeId}/link-user/{userId}")
    public ResponseEntity<Void> linkUser(@PathVariable Long employeeId, @PathVariable Long userId) {
        employeeService.linkUserToEmployee(employeeId, userId);
        return ResponseEntity.noContent().build();
    }
}