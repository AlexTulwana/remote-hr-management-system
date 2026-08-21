package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.DisciplinaryCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DisciplinaryCaseRepository extends JpaRepository<DisciplinaryCase, Long> {
    List<DisciplinaryCase> findByEmployeeId(Long employeeId);
    List<DisciplinaryCase> findByEmployeeBranchId(Long branchId);
    List<DisciplinaryCase> findByClosedFalse();
    List<DisciplinaryCase> findByEmployeeBranchIdAndClosedFalse(Long branchId);
}