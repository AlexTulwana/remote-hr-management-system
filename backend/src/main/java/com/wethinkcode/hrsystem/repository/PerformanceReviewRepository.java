package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.PerformanceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, Long> {
    List<PerformanceReview> findByEmployeeId(Long employeeId);
    List<PerformanceReview> findByEmployeeBranchId(Long branchId);
}