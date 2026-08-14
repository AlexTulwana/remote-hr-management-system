package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.HeadcountSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface HeadcountSnapshotRepository extends JpaRepository<HeadcountSnapshot, Long> {

    Optional<HeadcountSnapshot> findBySnapshotDateAndBranchIdAndDepartmentAndEmploymentStatus(
            LocalDate snapshotDate, Long branchId, String department, String employmentStatus);

    List<HeadcountSnapshot> findBySnapshotDateBetween(LocalDate start, LocalDate end);

    List<HeadcountSnapshot> findBySnapshotDateBetweenAndBranchId(LocalDate start, LocalDate end, Long branchId);

    List<HeadcountSnapshot> findBySnapshotDate(LocalDate snapshotDate);
}