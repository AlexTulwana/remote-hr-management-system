package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    List<Attendance> findByEmployeeId(Long employeeId);
    List<Attendance> findByEmployeeBranchId(Long branchId);
}
