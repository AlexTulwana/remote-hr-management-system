package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.PerformanceReviewRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.security.access.AccessDeniedException;



import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceReviewService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final CurrentUserService currentUserService;

    public PerformanceReviewService(PerformanceReviewRepository reviewRepository,
                                    EmployeeRepository employeeRepository,
                                    UserRepository userRepository,
                                    CurrentUserService currentUserService) {
        this.reviewRepository = reviewRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
        this.currentUserService = currentUserService;
    }

    public PerformanceReview create(PerformanceReviewRequest request) {
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));
        User reviewer = userRepository.findById(request.getReviewerId())
                .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        PerformanceReview review = new PerformanceReview();
        review.setEmployee(employee);
        review.setReviewer(reviewer);
        review.setCommunicationScore(request.getCommunicationScore());
        review.setTeamworkScore(request.getTeamworkScore());
        review.setProductivityScore(request.getProductivityScore());
        review.setAttendanceScore(request.getAttendanceScore());
        review.setComment(request.getComment());
        review.setReviewDate(LocalDate.now());

        return reviewRepository.save(review);
    }

    public List<PerformanceReview> getByEmployee(Long employeeId) {
        String role = currentUserService.getCurrentUser().getRole();
        if (!role.equals("HR") && !role.equals("ADMIN")) {
            if (!currentUserService.isSelf(employeeId)) {
                throw new AccessDeniedException("You are not authorized to view these performance reviews");
            }
        }
        return reviewRepository.findByEmployeeId(employeeId);
    }

    public List<PerformanceReview> getAll() {
        return reviewRepository.findAll();
    }
}