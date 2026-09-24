package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.time.LocalDateTime;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobPostingId(Long jobPostingId);
    List<Application> findByStatus(String status);
    List<Application> findByOutcome(String outcome);
    long countByJobPostingId(Long jobPostingId);

    List<Application> findBySubmittedAtBetween(LocalDateTime start, LocalDateTime end);
}