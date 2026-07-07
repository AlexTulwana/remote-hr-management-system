package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
}
