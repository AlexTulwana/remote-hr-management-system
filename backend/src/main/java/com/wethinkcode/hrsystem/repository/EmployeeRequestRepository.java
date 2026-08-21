package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.EmployeeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;


import java.util.List;

public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, Long> {
    List<EmployeeRequest> findByEmployeeId(Long employeeId);
    List<EmployeeRequest> findByStatus(String status);
    List<EmployeeRequest> findByEmployeeBranchId(Long branchId);


    @Query("SELECT r FROM EmployeeRequest r WHERE r.resolvedAt IS NOT NULL " +
            "AND r.resolvedAt BETWEEN :start AND :end")
    List<EmployeeRequest> findResolvedBetween(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
    List<EmployeeRequest> findByStatusAndEmployeeBranchId(String status, Long branchId);
}
