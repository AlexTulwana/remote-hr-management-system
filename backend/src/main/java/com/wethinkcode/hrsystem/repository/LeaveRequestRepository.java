package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByEmployeeId(Long employeeId);
    List<LeaveRequest> findByEmployeeBranchId(Long branchId);
    List<LeaveRequest> findByStatusAndEmployeeBranchId(String status, Long branchId);
    List<LeaveRequest> findByEmployeeIdAndStatusIn(Long employeeId, List<String> statuses);
    List<LeaveRequest> findByStatusIn(List<String> statuses);
}
