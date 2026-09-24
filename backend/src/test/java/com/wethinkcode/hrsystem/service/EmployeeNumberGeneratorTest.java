package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeNumberGeneratorTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @InjectMocks
    private EmployeeNumberGenerator generator;

    private Employee emp(String number) {
        Employee e = new Employee();
        e.setEmployeeNumber(number);
        return e;
    }

    @Test
    void next_branchHasEmployees_continuesThatBranchSeries() {
        when(employeeRepository.findByBranchId(1L))
                .thenReturn(List.of(emp("EMP-1001"), emp("EMP-2022"), emp("EMP-2003")));

        assertEquals("EMP-2023", generator.next(1L));
    }

    @Test
    void next_noBranch_continuesNoBranchStaffSeries() {
        when(employeeRepository.findByBranchIsNull())
                .thenReturn(List.of(emp("EMP-1003"), emp("EMP-4001")));

        assertEquals("EMP-4002", generator.next(null));
    }

    @Test
    void next_branchWithNoEmployees_startsNextFreeSeries() {
        when(employeeRepository.findByBranchId(9L)).thenReturn(List.of());
        when(employeeRepository.findAll())
                .thenReturn(List.of(emp("EMP-2022"), emp("EMP-3024"), emp("EMP-4001")));

        assertEquals("EMP-5001", generator.next(9L));
    }

    @Test
    void next_noEmployeesAtAll_startsAt1001() {
        assertEquals("EMP-1001", generator.next(1L));
    }

    @Test
    void next_skipsNumbersAlreadyTaken() {
        when(employeeRepository.findByBranchId(1L)).thenReturn(List.of(emp("EMP-2022")));
        when(employeeRepository.existsByEmployeeNumber("EMP-2023")).thenReturn(true);

        assertEquals("EMP-2024", generator.next(1L));
    }

    @Test
    void next_ignoresMalformedNumbers() {
        when(employeeRepository.findByBranchId(1L))
                .thenReturn(List.of(emp("ABC"), emp(null), emp("EMP-2005")));

        assertEquals("EMP-2006", generator.next(1L));
    }
}
