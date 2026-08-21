package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Hearing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface HearingRepository extends JpaRepository<Hearing, Long> {
    List<Hearing> findByEmployeeId(Long employeeId);
    List<Hearing> findByEmployeeBranchIdAndHearingDateTimeBetweenAndStatus(
            Long branchId, LocalDateTime start, LocalDateTime end, String status);
}