package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.HeadcountSnapshot;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.HeadcountAggregateProjection;
import com.wethinkcode.hrsystem.repository.HeadcountSnapshotRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class HeadcountSnapshotService {

    private final EmployeeRepository employeeRepository;
    private final HeadcountSnapshotRepository snapshotRepository;

    public HeadcountSnapshotService(EmployeeRepository employeeRepository,
                                    HeadcountSnapshotRepository snapshotRepository) {
        this.employeeRepository = employeeRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Scheduled(cron = "0 5 0 * * *")
    public void snapshotHeadcountNightly() {
        takeSnapshot(LocalDate.now());
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
}