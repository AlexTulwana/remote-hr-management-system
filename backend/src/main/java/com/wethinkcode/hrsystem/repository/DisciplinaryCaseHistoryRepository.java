package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.DisciplinaryCaseHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisciplinaryCaseHistoryRepository extends JpaRepository<DisciplinaryCaseHistory, Long> {
    List<DisciplinaryCaseHistory> findByDisciplinaryCaseIdOrderByActionedAtAsc(Long caseId);
}
