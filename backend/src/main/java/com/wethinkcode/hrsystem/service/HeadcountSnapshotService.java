package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.HeadcountSnapshot;
import com.wethinkcode.hrsystem.model.SalarySnapshot;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection;
import com.wethinkcode.hrsystem.repository.HeadcountSnapshotRepository;
import com.wethinkcode.hrsystem.repository.SalaryAggregateProjection;
import com.wethinkcode.hrsystem.repository.SalarySnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HeadcountSnapshotService {

    private final EmployeeRepository employeeRepository;
    private final HeadcountSnapshotRepository snapshotRepository;
    private final SalarySnapshotRepository salarySnapshotRepository;

    public HeadcountSnapshotService(EmployeeRepository employeeRepository,
                                    HeadcountSnapshotRepository snapshotRepository,
                                    SalarySnapshotRepository salarySnapshotRepository) {
        this.employeeRepository = employeeRepository;
        this.snapshotRepository = snapshotRepository;
        this.salarySnapshotRepository = salarySnapshotRepository;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void snapshotHeadcountNightly() {
        takeSnapshot(LocalDate.now());
        takeSalarySnapshot(LocalDate.now());
    }

    // Manually triggerable version, e.g. for backfilling today's data immediately
    public int takeSnapshot(LocalDate date) {
        List<HeadcountAggregateProjection> aggregates = employeeRepository.aggregateByBranchDeptStatus();

        for (HeadcountAggregateProjection agg : aggregates) {
            HeadcountSnapshot snapshot = snapshotRepository
                    .findBySnapshotDateAndBranchIdAndDepartmentAndEmploymentStatus(
                            date, agg.getBranchId(), agg.getDepartment(), agg.getEmploymentStatus())
                    .orElseGet(HeadcountSnapshot::new);

            snapshot.setSnapshotDate(date);
            snapshot.setBranchId(agg.getBranchId());
            snapshot.setDepartment(agg.getDepartment());
            snapshot.setEmploymentStatus(agg.getEmploymentStatus());
            snapshot.setHeadcount(agg.getHeadcount().intValue());

            snapshotRepository.save(snapshot);
        }

        return aggregates.size();
    }

    // Manually triggerable version, mirrors takeSnapshot() but for salary data
    public int takeSalarySnapshot(LocalDate date) {
        List<SalaryAggregateProjection> aggregates = employeeRepository.aggregateSalaryByBranchDept();

        for (SalaryAggregateProjection agg : aggregates) {
            SalarySnapshot snapshot = salarySnapshotRepository
                    .findBySnapshotDateAndBranchIdAndDepartment(date, agg.getBranchId(), agg.getDepartment())
                    .orElseGet(SalarySnapshot::new);

            snapshot.setSnapshotDate(date);
            snapshot.setBranchId(agg.getBranchId());
            snapshot.setDepartment(agg.getDepartment());
            snapshot.setTotalSalary(agg.getTotalSalary());
            snapshot.setAverageSalary(agg.getAverageSalary());
            snapshot.setEmployeeCount(agg.getEmployeeCount().intValue());

            salarySnapshotRepository.save(snapshot);
        }

        return aggregates.size();
    }
}