package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.ApplicationDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ApplicationDocumentRepository extends JpaRepository<ApplicationDocument, Long> {
    List<ApplicationDocument> findByApplicationId(Long applicationId);
}
