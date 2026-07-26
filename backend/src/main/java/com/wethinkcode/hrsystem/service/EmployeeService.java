package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.BranchRepository;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Service;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;


import java.util.List;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;

    public EmployeeService(EmployeeRepository employeeRepository, BranchRepository branchRepository, UserRepository userRepository) {
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
        this.userRepository = userRepository;
    }

    public Employee create(EmployeeRequest request) {
        Employee employee = new Employee();
        mapRequestToEmployee(employee, request);
        return employeeRepository.save(employee);
    }

    public List<Employee> getAll() {
        return employeeRepository.findAll();
    }

    public Employee getById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Employee not found"));
    }

    public Employee update(Long id, EmployeeRequest request) {
        Employee employee = getById(id);
        mapRequestToEmployee(employee, request);
        return employeeRepository.save(employee);
    }
    public void linkUserToEmployee(Long employeeId, Long userId) {
        Employee employee = getById(employeeId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setEmployee(employee);
        userRepository.save(user);
    }

    public void deactivate(Long id) {
        Employee employee = getById(id);
        employee.setActive(false);
        employeeRepository.save(employee);
    }

    private void mapRequestToEmployee(Employee employee, EmployeeRequest request) {
        employee.setEmployeeNumber(request.getEmployeeNumber());
        employee.setFullName(request.getFullName());
        employee.setPosition(request.getPosition());
        employee.setDepartment(request.getDepartment());
        employee.setQualifications(request.getQualifications());
        employee.setContactDetails(request.getContactDetails());
        employee.setEmploymentDate(request.getEmploymentDate());

        if (request.getBranchId() != null) {
            Branch branch = branchRepository.findById(request.getBranchId())
                    .orElseThrow(() -> new RuntimeException("Branch not found"));
            employee.setBranch(branch);
        }
    }
}
