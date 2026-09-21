package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.model.Branch;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.PerformanceReviewRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PerformanceReviewServiceTest {

    @Mock
    private PerformanceReviewRepository reviewRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private PerformanceReviewService performanceReviewService;

    private Employee employee;
    private User reviewer;
    private PerformanceReviewRequest request;

    @BeforeEach
    void setUp() {
        employee = new Employee();
        employee.setId(1L);

        reviewer = new User();
        reviewer.setId(2L);

        request = new PerformanceReviewRequest();
        request.setEmployeeId(1L);
        request.setCommunicationScore(4);
        request.setTeamworkScore(5);
        request.setProductivityScore(3);
        request.setAttendanceScore(5);
        request.setComment("Solid quarter overall.");
    }

    // ---- create() ----

    private Branch branch(Long id) {
        Branch b = new Branch();
        b.setId(id);
        return b;
    }

    private User managerIn(Long branchId) {
        Employee managerEmployee = new Employee();
        managerEmployee.setId(3L);
        managerEmployee.setBranch(branch(branchId));
        User manager = new User();
        manager.setId(5L);
        manager.setUsername("mgrtest1");
        manager.setRole("MANAGER");
        manager.setEmployee(managerEmployee);
        return manager;
    }

    private User hrUser() {
        User hr = new User();
        hr.setId(6L);
        hr.setUsername("hrtest2");
        hr.setRole("HR");
        return hr;
    }

    @Test
    void create_managerSameBranch_usesLoggedInUserAsReviewer() {
        User manager = managerIn(1L);
        employee.setBranch(branch(1L));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reviewRepository.save(any(PerformanceReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceReview result = performanceReviewService.create(request, "mgrtest1");

        assertEquals(employee, result.getEmployee());
        assertEquals(manager, result.getReviewer());
        assertEquals(4, result.getCommunicationScore());
        assertEquals(5, result.getTeamworkScore());
        assertEquals(3, result.getProductivityScore());
        assertEquals(5, result.getAttendanceScore());
        assertEquals("Solid quarter overall.", result.getComment());
        assertEquals(LocalDate.now(), result.getReviewDate());
    }

    @Test
    void create_hr_canReviewAnyBranch() {
        User hr = hrUser();
        employee.setBranch(branch(2L));
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hr));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reviewRepository.save(any(PerformanceReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals(hr, performanceReviewService.create(request, "hrtest2").getReviewer());
    }

    @Test
    void create_managerOtherBranch_throwsAccessDenied() {
        employee.setBranch(branch(2L));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(managerIn(1L)));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> performanceReviewService.create(request, "mgrtest1"));
        assertEquals("Managers can only review employees in their own branch", ex.getMessage());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_managerWithNoBranch_throwsAccessDenied() {
        User manager = managerIn(1L);
        manager.getEmployee().setBranch(null);
        employee.setBranch(branch(1L));
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(AccessDeniedException.class,
                () -> performanceReviewService.create(request, "mgrtest1"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_reviewingYourself_throwsAccessDenied() {
        User manager = managerIn(1L);
        request.setEmployeeId(3L);
        when(userRepository.findByUsername("mgrtest1")).thenReturn(Optional.of(manager));
        when(employeeRepository.findById(3L)).thenReturn(Optional.of(manager.getEmployee()));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                () -> performanceReviewService.create(request, "mgrtest1"));
        assertEquals("You cannot review yourself", ex.getMessage());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_scoreBelowOne_throwsAndDoesNotSave() {
        request.setCommunicationScore(0);
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser()));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> performanceReviewService.create(request, "hrtest2"));
        assertEquals("Scores must be between 1 and 5", ex.getMessage());

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_scoreAboveFive_throwsAndDoesNotSave() {
        request.setTeamworkScore(6);
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser()));
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(RuntimeException.class, () -> performanceReviewService.create(request, "hrtest2"));

        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_userNotFound_throwsRuntimeException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> performanceReviewService.create(request, "ghost"));
        assertEquals("User not found", ex.getMessage());

        verify(employeeRepository, never()).findById(any());
    }

    @Test
    void create_employeeNotFound_throwsRuntimeException() {
        when(userRepository.findByUsername("hrtest2")).thenReturn(Optional.of(hrUser()));
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> performanceReviewService.create(request, "hrtest2"));
        assertEquals("Employee not found", ex.getMessage());

        verify(reviewRepository, never()).save(any());
    }

    // ---- getByEmployee() ----

    @Test
    void getByEmployee_asHR_returnsReviewsWithoutSelfCheck() {
        User hrUser = new User();
        hrUser.setRole("HR");
        when(currentUserService.getCurrentUser()).thenReturn(hrUser);
        when(reviewRepository.findByEmployeeId(1L)).thenReturn(List.of(new PerformanceReview()));

        List<PerformanceReview> result = performanceReviewService.getByEmployee(1L);

        assertEquals(1, result.size());
        verify(currentUserService, never()).isSelf(any());
    }

    @Test
    void getByEmployee_asAdmin_returnsReviewsWithoutSelfCheck() {
        User adminUser = new User();
        adminUser.setRole("ADMIN");
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        when(reviewRepository.findByEmployeeId(1L)).thenReturn(List.of(new PerformanceReview()));

        List<PerformanceReview> result = performanceReviewService.getByEmployee(1L);

        assertEquals(1, result.size());
        verify(currentUserService, never()).isSelf(any());
    }

    @Test
    void getByEmployee_asEmployeeViewingSelf_returnsReviews() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(true);
        when(reviewRepository.findByEmployeeId(1L)).thenReturn(List.of(new PerformanceReview()));

        List<PerformanceReview> result = performanceReviewService.getByEmployee(1L);

        assertEquals(1, result.size());
    }

    @Test
    void getByEmployee_asEmployeeViewingOther_throwsAccessDenied() {
        User employeeUser = new User();
        employeeUser.setRole("EMPLOYEE");
        when(currentUserService.getCurrentUser()).thenReturn(employeeUser);
        when(currentUserService.isSelf(1L)).thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> performanceReviewService.getByEmployee(1L));

        verify(reviewRepository, never()).findByEmployeeId(any());
    }

    // ---- getAll() ----

    @Test
    void getAll_returnsAllReviews() {
        List<PerformanceReview> reviews = Arrays.asList(new PerformanceReview(), new PerformanceReview());
        when(reviewRepository.findAll()).thenReturn(reviews);

        List<PerformanceReview> result = performanceReviewService.getAll();

        assertEquals(2, result.size());
        verify(reviewRepository).findAll();
    }

    // ---- getByEmployee() as manager ----

    @Test
    void getByEmployee_managerSameBranch_returnsReviews() {
        User manager = new User();
        manager.setRole("MANAGER");
        employee.setBranch(branch(1L));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(reviewRepository.findByEmployeeId(1L)).thenReturn(List.of(new PerformanceReview()));

        assertEquals(1, performanceReviewService.getByEmployee(1L).size());
    }

    @Test
    void getByEmployee_managerOtherBranch_throwsAccessDenied() {
        User manager = new User();
        manager.setRole("MANAGER");
        employee.setBranch(branch(2L));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(AccessDeniedException.class, () -> performanceReviewService.getByEmployee(1L));

        verify(reviewRepository, never()).findByEmployeeId(any());
    }

    @Test
    void getByEmployee_managerWithNoBranch_throwsAccessDenied() {
        User manager = new User();
        manager.setRole("MANAGER");
        employee.setBranch(branch(1L));
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.getCurrentBranchId()).thenReturn(null);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));

        assertThrows(AccessDeniedException.class, () -> performanceReviewService.getByEmployee(1L));
    }

    @Test
    void getByEmployee_managerViewingSelf_returnsReviewsWithoutBranchLookup() {
        User manager = new User();
        manager.setRole("MANAGER");
        when(currentUserService.getCurrentUser()).thenReturn(manager);
        when(currentUserService.isSelf(1L)).thenReturn(true);
        when(reviewRepository.findByEmployeeId(1L)).thenReturn(List.of(new PerformanceReview()));

        assertEquals(1, performanceReviewService.getByEmployee(1L).size());
        verify(employeeRepository, never()).findById(any());
    }

    // ---- getByBranch() ----

    @Test
    void getByBranch_managerOwnBranch_returnsReviews() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);
        when(reviewRepository.findByEmployeeBranchId(1L)).thenReturn(List.of(new PerformanceReview()));

        assertEquals(1, performanceReviewService.getByBranch(1L).size());
    }

    @Test
    void getByBranch_managerOtherBranch_throwsAccessDeniedWithoutQuerying() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(1L);

        assertThrows(AccessDeniedException.class, () -> performanceReviewService.getByBranch(2L));

        verify(reviewRepository, never()).findByEmployeeBranchId(any());
    }

    @Test
    void getByBranch_managerWithNoBranch_throwsAccessDenied() {
        when(currentUserService.isHrOrAdmin()).thenReturn(false);
        when(currentUserService.getCurrentBranchId()).thenReturn(null);

        assertThrows(AccessDeniedException.class, () -> performanceReviewService.getByBranch(1L));
    }

    @Test
    void getByBranch_hr_anyBranch() {
        when(currentUserService.isHrOrAdmin()).thenReturn(true);
        when(reviewRepository.findByEmployeeBranchId(2L)).thenReturn(List.of(new PerformanceReview()));

        assertEquals(1, performanceReviewService.getByBranch(2L).size());
        verify(currentUserService, never()).getCurrentBranchId();
    }
}
