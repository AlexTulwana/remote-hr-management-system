package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.HeadcountSnapshot;
import com.wethinkcode.hrsystem.model.SalarySnapshot;
import com.wethinkcode.hrsystem.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HeadcountSnapshotServiceTest {

    @Mock private EmployeeRepository employeeRepository;
    @Mock private HeadcountSnapshotRepository snapshotRepository;
    @Mock private SalarySnapshotRepository salarySnapshotRepository;

    @InjectMocks
    private HeadcountSnapshotService headcountSnapshotService;

    private final LocalDate date = LocalDate.of(2026, 1, 15);

    private HeadcountAggregateProjection mockHeadcountAgg(Long branchId, String dept, String status, Long headcount) {
        HeadcountAggregateProjection agg = mock(HeadcountAggregateProjection.class);
        when(agg.getBranchId()).thenReturn(branchId);
        when(agg.getDepartment()).thenReturn(dept);
        when(agg.getEmploymentStatus()).thenReturn(status);
        when(agg.getHeadcount()).thenReturn(headcount);
        return agg;
    }

    private SalaryAggregateProjection mockSalaryAgg(Long branchId, String dept, Double total, Double avg, Long count) {
        SalaryAggregateProjection agg = mock(SalaryAggregateProjection.class);
        when(agg.getBranchId()).thenReturn(branchId);
        when(agg.getDepartment()).thenReturn(dept);
        when(agg.getTotalSalary()).thenReturn(total);
        when(agg.getAverageSalary()).thenReturn(avg);
        when(agg.getEmployeeCount()).thenReturn(count);
        return agg;
    }

    // ---------- takeSnapshot() ----------

    @Test
    void takeSnapshot_newSnapshot_createsAndSaves() {
        HeadcountAggregateProjection agg = mockHeadcountAgg(1L, "IT", "ACTIVE", 5L);
        when(employeeRepository.aggregateByBranchDeptStatus()).thenReturn(List.of(agg));
        when(snapshotRepository.findBySnapshotDateAndBranchIdAndDepartmentAndEmploymentStatus(
                date, 1L, "IT", "ACTIVE")).thenReturn(Optional.empty());
        when(snapshotRepository.save(any(HeadcountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = headcountSnapshotService.takeSnapshot(date);

        assertEquals(1, count);
        verify(snapshotRepository).save(argThat(s ->
                s.getBranchId().equals(1L) && s.getDepartment().equals("IT")
                        && s.getEmploymentStatus().equals("ACTIVE") && s.getHeadcount() == 5));
    }

    @Test
    void takeSnapshot_existingSnapshot_updatesInPlace() {
        HeadcountAggregateProjection agg = mockHeadcountAgg(1L, "IT", "ACTIVE", 8L);
        HeadcountSnapshot existing = new HeadcountSnapshot();
        existing.setId(99L);
        existing.setHeadcount(5);

        when(employeeRepository.aggregateByBranchDeptStatus()).thenReturn(List.of(agg));
        when(snapshotRepository.findBySnapshotDateAndBranchIdAndDepartmentAndEmploymentStatus(
                date, 1L, "IT", "ACTIVE")).thenReturn(Optional.of(existing));
        when(snapshotRepository.save(any(HeadcountSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        headcountSnapshotService.takeSnapshot(date);

        verify(snapshotRepository).save(argThat(s -> s.getId().equals(99L) && s.getHeadcount() == 8));
    }

    @Test
    void takeSnapshot_noAggregates_returnsZero() {
        when(employeeRepository.aggregateByBranchDeptStatus()).thenReturn(List.of());

        assertEquals(0, headcountSnapshotService.takeSnapshot(date));
        verifyNoInteractions(snapshotRepository);
    }

    // ---------- takeSalarySnapshot() ----------

    @Test
    void takeSalarySnapshot_newSnapshot_createsAndSaves() {
        SalaryAggregateProjection agg = mockSalaryAgg(1L, "IT", 100000.0, 50000.0, 2L);
        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of(agg));
        when(salarySnapshotRepository.findBySnapshotDateAndBranchIdAndDepartment(date, 1L, "IT"))
                .thenReturn(Optional.empty());
        when(salarySnapshotRepository.save(any(SalarySnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        int count = headcountSnapshotService.takeSalarySnapshot(date);

        assertEquals(1, count);
        verify(salarySnapshotRepository).save(argThat(s ->
                s.getTotalSalary().equals(100000.0) && s.getEmployeeCount() == 2));
    }

    @Test
    void takeSalarySnapshot_existingSnapshot_updatesInPlace() {
        SalaryAggregateProjection agg = mockSalaryAgg(1L, "IT", 120000.0, 60000.0, 2L);
        SalarySnapshot existing = new SalarySnapshot();
        existing.setId(55L);

        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of(agg));
        when(salarySnapshotRepository.findBySnapshotDateAndBranchIdAndDepartment(date, 1L, "IT"))
                .thenReturn(Optional.of(existing));
        when(salarySnapshotRepository.save(any(SalarySnapshot.class))).thenAnswer(inv -> inv.getArgument(0));

        headcountSnapshotService.takeSalarySnapshot(date);

        verify(salarySnapshotRepository).save(argThat(s -> s.getId().equals(55L) && s.getTotalSalary().equals(120000.0)));
    }

    @Test
    void takeSalarySnapshot_noAggregates_returnsZero() {
        when(employeeRepository.aggregateSalaryByBranchDept()).thenReturn(List.of());

        assertEquals(0, headcountSnapshotService.takeSalarySnapshot(date));
        verifyNoInteractions(salarySnapshotRepository);
    }
}