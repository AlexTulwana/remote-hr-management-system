package com.wethinkcode.hrsystem.repository;

import com.wethinkcode.hrsystem.model.Employee;
import org.springframework.data.jpa.domain.Specification;

public class EmployeeSpecifications {

    public static Specification<Employee> withFilters(Long branchId, String department, String employmentStatus) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();
            if (branchId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("branch").get("id"), branchId));
            }
            if (department != null && !department.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("department"), department));
            }
            if (employmentStatus != null && !employmentStatus.isBlank()) {
                predicates = cb.and(predicates, cb.equal(root.get("employmentStatus"), employmentStatus));
            }
            return predicates;
        };
    }
}