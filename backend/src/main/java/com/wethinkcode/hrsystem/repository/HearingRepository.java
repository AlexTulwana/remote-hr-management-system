package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Hearing;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HearingRepository extends JpaRepository<Hearing, Long> {
    List<Hearing> findByEmployeeId(Long employeeId);
}