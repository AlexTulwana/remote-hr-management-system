package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.EmployeeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, Long> {
    List<EmployeeRequest> findByEmployeeId(Long employeeId);
    List<EmployeeRequest> findByStatus(String status);
    List<EmployeeRequest> findByEmployeeBranchId(Long branchId);
}
