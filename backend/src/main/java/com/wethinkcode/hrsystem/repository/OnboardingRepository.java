package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Onboarding;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OnboardingRepository extends JpaRepository<Onboarding, Long> {
    List<Onboarding> findByEmployeeId(Long employeeId);
    List<Onboarding> findByStatus(String status);
    boolean existsByApplicationId(Long applicationId);
}