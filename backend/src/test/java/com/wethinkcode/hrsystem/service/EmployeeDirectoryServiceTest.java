package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.EmployeeDirectoryEntry;
import com.wethinkcode.hrsystem.dto.OrgChartNode;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeDirectoryServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private EmployeeDirectoryService employeeDirectoryService;

    private Branch branch;

    @BeforeEach
    void setUp() {
        branch = new Branch();
        branch.setId(1L);
        branch.setName("Cape Town");
        lenient().when(currentUserService.isHrOrAdmin()).thenReturn(true);
    }

    // ---------- getDirectory() ----------

    @Test
    void getDirectory_mapsEmployeesToEntries() {
        Employee e1 = new Employee();
        e1.setId(1L);
        e1.setFullName("Emma Employee");
        e1.setBranch(branch);

        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(e1));

        List<EmployeeDirectoryEntry> result = employeeDirectoryService.getDirectory(1L, "IT", "ACTIVE");

        assertEquals(1, result.size());
        assertEquals("Emma Employee", result.get(0).getFullName());
        assertEquals("Cape Town", result.get(0).getBranchName());
    }

    @Test
    void getDirectory_noMatches_returnsEmptyList() {
        when(employeeRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of());

        assertTrue(employeeDirectoryService.getDirectory(null, null, null).isEmpty());
    }

    // ---------- getOrgChart() ----------

    @Test
    void getOrgChart_buildsTreeFromRoots() {
        Employee root = new Employee();
        root.setId(1L);
        root.setFullName("Boss");
        root.setBranch(branch);

        Employee report = new Employee();
        report.setId(2L);
        report.setFullName("Subordinate");
        report.setBranch(branch);

        when(employeeRepository.findByReportsToIsNull()).thenReturn(List.of(root));
        when(employeeRepository.findByReportsToId(1L)).thenReturn(List.of(report));
        when(employeeRepository.findByReportsToId(2L)).thenReturn(List.of());

        List<OrgChartNode> result = employeeDirectoryService.getOrgChart();

        assertEquals(1, result.size());
        assertEquals("Boss", result.get(0).getFullName());
        assertEquals(1, result.get(0).getDirectReports().size());
        assertEquals("Subordinate", result.get(0).getDirectReports().get(0).getFullName());
        assertTrue(result.get(0).getDirectReports().get(0).getDirectReports().isEmpty());
    }

    @Test
    void getOrgChart_noRoots_returnsEmptyList() {
        when(employeeRepository.findByReportsToIsNull()).thenReturn(List.of());

        assertTrue(employeeDirectoryService.getOrgChart().isEmpty());
    }

    // ---------- getOrgChartFrom() ----------

    @Test
    void getOrgChartFrom_notFound_throwsException() {
        when(employeeRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> employeeDirectoryService.getOrgChartFrom(99L));
    }

    @Test
    void getOrgChartFrom_found_buildsSubtree() {
        Employee root = new Employee();
        root.setId(5L);
        root.setFullName("Team Lead");
        root.setBranch(branch);

        when(employeeRepository.findById(5L)).thenReturn(Optional.of(root));
        when(employeeRepository.findByReportsToId(5L)).thenReturn(List.of());

        OrgChartNode result = employeeDirectoryService.getOrgChartFrom(5L);

        assertEquals("Team Lead", result.getFullName());
        assertTrue(result.getDirectReports().isEmpty());
    }
}