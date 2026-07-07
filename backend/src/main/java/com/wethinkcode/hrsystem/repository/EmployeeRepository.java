package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
