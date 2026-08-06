package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, Long> {
    List<EmployeeDocument> findByEmployeeIdAndDeletedFalse(Long employeeId);
}