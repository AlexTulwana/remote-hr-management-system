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

    public PerformanceReview create(PerformanceReviewRequest request, String username) {
        User reviewer = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        Employee employee = employeeRepository.findById(request.getEmployeeId())
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (reviewer.getEmployee() != null && reviewer.getEmployee().getId() != null
                && reviewer.getEmployee().getId().equals(employee.getId())) {
            throw new AccessDeniedException("You cannot review yourself");
        }

        if ("MANAGER".equals(reviewer.getRole())) {
            Long managerBranchId = reviewer.getEmployee() != null && reviewer.getEmployee().getBranch() != null
                    ? reviewer.getEmployee().getBranch().getId() : null;
            Long employeeBranchId = employee.getBranch() != null ? employee.getBranch().getId() : null;
            if (managerBranchId == null || !managerBranchId.equals(employeeBranchId)) {
                throw new AccessDeniedException("Managers can only review employees in their own branch");
            }
        }

        requireValidScore(request.getCommunicationScore());
        requireValidScore(request.getTeamworkScore());
        requireValidScore(request.getProductivityScore());
        requireValidScore(request.getAttendanceScore());

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

    private void requireValidScore(int score) {
        if (score < 1 || score > 5) {
            throw new RuntimeException("Scores must be between 1 and 5");
        }
    }

    public List<PerformanceReview> getByEmployee(Long employeeId) {
        String role = currentUserService.getCurrentUser().getRole();
        if ("HR".equals(role) || "ADMIN".equals(role) || currentUserService.isSelf(employeeId)) {
            return reviewRepository.findByEmployeeId(employeeId);
        }
        if ("MANAGER".equals(role)) {
            Employee target = employeeRepository.findById(employeeId)
                    .orElseThrow(() -> new RuntimeException("Employee not found"));
            Long managerBranchId = currentUserService.getCurrentBranchId();
            Long targetBranchId = target.getBranch() != null ? target.getBranch().getId() : null;
            if (managerBranchId != null && managerBranchId.equals(targetBranchId)) {
                return reviewRepository.findByEmployeeId(employeeId);
            }
        }
        throw new AccessDeniedException("You are not authorized to view these performance reviews");
    }

    public List<PerformanceReview> getByBranch(Long branchId) {
        if (!currentUserService.isHrOrAdmin()) {
            Long callerBranchId = currentUserService.getCurrentBranchId();
            if (callerBranchId == null || !callerBranchId.equals(branchId)) {
                throw new AccessDeniedException("You can only view reviews for your own branch");
            }
        }
        return reviewRepository.findByEmployeeBranchId(branchId);
    }

    public List<PerformanceReview> getAll() {
        return reviewRepository.findAll();
    }
}