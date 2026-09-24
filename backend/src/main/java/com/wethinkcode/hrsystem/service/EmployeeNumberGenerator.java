package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class EmployeeNumberGenerator {

    private static final Pattern NUMBER = Pattern.compile("^EMP-(\\d{1,9})$");
    private static final int SERIES_SIZE = 1000;

    private final EmployeeRepository employeeRepository;

    public EmployeeNumberGenerator(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    // Continues the highest number used in the same branch (or among no-branch staff).
    // A group with no employees yet starts the next unused thousand-series.
    public String next(Long branchId) {
        List<Employee> group = branchId != null
                ? employeeRepository.findByBranchId(branchId)
                : employeeRepository.findByBranchIsNull();

        int candidate = highest(group);
        if (candidate == 0) {
            int highestSeries = highest(employeeRepository.findAll()) / SERIES_SIZE;
            candidate = (highestSeries + 1) * SERIES_SIZE + 1;
        } else {
            candidate = candidate + 1;
        }

        while (employeeRepository.existsByEmployeeNumber(format(candidate))) {
            candidate++;
        }
        return format(candidate);
    }

    private int highest(List<Employee> employees) {
        int max = 0;
        for (Employee e : employees) {
            if (e.getEmployeeNumber() == null) {
                continue;
            }
            Matcher m = NUMBER.matcher(e.getEmployeeNumber());
            if (m.matches()) {
                max = Math.max(max, Integer.parseInt(m.group(1)));
            }
        }
        return max;
    }

    private String format(int number) {
        return String.format("EMP-%04d", number);
    }
}
