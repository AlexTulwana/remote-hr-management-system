package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    @Query("SELECT e.branch.id as branchId, e.department as department, e.employmentStatus as employmentStatus, COUNT(e) as headcount " +
            "FROM Employee e GROUP BY e.branch.id, e.department, e.employmentStatus")
    List<HeadcountAggregateProjection> aggregateByBranchDeptStatus();

    @Query("SELECT e.branch.id as branchId, e.department as department, " +
            "SUM(e.salary) as totalSalary, AVG(e.salary) as averageSalary, COUNT(e) as employeeCount " +
            "FROM Employee e WHERE e.salary IS NOT NULL AND e.employmentStatus = 'ACTIVE' " +
            "GROUP BY e.branch.id, e.department")
    List<SalaryAggregateProjection> aggregateSalaryByBranchDept();

}