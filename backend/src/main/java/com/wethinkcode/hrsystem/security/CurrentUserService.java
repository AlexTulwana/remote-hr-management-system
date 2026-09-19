package com.wethinkcode.hrsystem.security;

import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserService {

    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Current user not found"));
    }

    public boolean isSelf(Long employeeId) {
        User current = getCurrentUser();
        return current.getEmployee() != null && current.getEmployee().getId().equals(employeeId);
    }

    public boolean isHrOrAdmin() {
        String role = getCurrentUser().getRole();
        return "HR".equals(role) || "ADMIN".equals(role);
    }

    public Long getCurrentBranchId() {
        User current = getCurrentUser();
        return current.getEmployee() != null && current.getEmployee().getBranch() != null
                ? current.getEmployee().getBranch().getId() : null;
    }
}