package com.wethinkcode.hrsystem.service;

import com.wethinkcode.hrsystem.dto.PerformanceReviewRequest;
import com.wethinkcode.hrsystem.model.Employee;
import com.wethinkcode.hrsystem.model.PerformanceReview;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.EmployeeRepository;
import com.wethinkcode.hrsystem.repository.PerformanceReviewRepository;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class PerformanceReviewService {

    private final PerformanceReviewRepository reviewRepository;
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;

    public PerformanceReviewService(PerformanceReviewRepository reviewRepository,
                                    EmployeeRepository employeeRepository,
                                    UserRepository userRepository) {
        this.reviewRepository = reviewRepository;
        this.employeeRepository = employeeRepository;
        this.userRepository = userRepository;
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
        return reviewRepository.findByEmployeeId(employeeId);
    }

    public List<PerformanceReview> getAll() {
        return reviewRepository.findAll();
    }
}