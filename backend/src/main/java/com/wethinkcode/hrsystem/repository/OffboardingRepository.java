package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Offboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OffboardingRepository extends JpaRepository<Offboarding, Long> {
    List<Offboarding> findByEmployeeId(Long employeeId);
    List<Offboarding> findByStatus(String status);
}