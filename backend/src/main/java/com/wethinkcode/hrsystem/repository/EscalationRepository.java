package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Escalation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findByType(String type);
    List<Escalation> findByStatus(String status);
}
