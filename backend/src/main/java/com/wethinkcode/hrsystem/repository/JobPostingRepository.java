package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.JobPosting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JobPostingRepository extends JpaRepository<JobPosting, Long> {
    List<JobPosting> findByDepartment(String department);
    long countByBranchId(Long branchId);
}