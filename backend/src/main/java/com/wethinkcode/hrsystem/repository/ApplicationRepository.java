package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByJobPostingId(Long jobPostingId);
    List<Application> findByStatus(String status);
    long countByJobPostingId(Long jobPostingId);
}