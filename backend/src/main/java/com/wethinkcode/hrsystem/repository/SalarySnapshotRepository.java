package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.SalarySnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SalarySnapshotRepository extends JpaRepository<SalarySnapshot, Long> {

    Optional<SalarySnapshot> findBySnapshotDateAndBranchIdAndDepartment(
            LocalDate snapshotDate, Long branchId, String department);

    List<SalarySnapshot> findBySnapshotDateBetween(LocalDate start, LocalDate end);
}