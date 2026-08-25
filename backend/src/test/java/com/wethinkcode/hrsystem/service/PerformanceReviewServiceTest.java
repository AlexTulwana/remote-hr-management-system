package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
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
        request.setReviewerId(2L);
        request.setCommunicationScore(4);
        request.setTeamworkScore(5);
        request.setProductivityScore(3);
        request.setAttendanceScore(5);
        request.setComment("Solid quarter overall.");
    }

    // ---- create() ----

    @Test
    void create_savesReviewWithMappedFieldsAndTodayDate() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.of(reviewer));
        when(reviewRepository.save(any(PerformanceReview.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        PerformanceReview result = performanceReviewService.create(request);

        assertNotNull(result);
        assertEquals(employee, result.getEmployee());
        assertEquals(reviewer, result.getReviewer());
        assertEquals(4, result.getCommunicationScore());
        assertEquals(5, result.getTeamworkScore());
        assertEquals(3, result.getProductivityScore());
        assertEquals(5, result.getAttendanceScore());
        assertEquals("Solid quarter overall.", result.getComment());
        assertEquals(LocalDate.now(), result.getReviewDate());

        verify(reviewRepository).save(any(PerformanceReview.class));
    }

    @Test
    void create_employeeNotFound_throwsRuntimeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> performanceReviewService.create(request));
        assertEquals("Employee not found", ex.getMessage());

        verify(userRepository, never()).findById(any());
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void create_reviewerNotFound_throwsRuntimeException() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(employee));
        when(userRepository.findById(2L)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> performanceReviewService.create(request));
        assertEquals("Reviewer not found", ex.getMessage());

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
}