package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.CurrentUserResponse;
import com.wethinkcode.hrsystem.dto.MessageRecipientOption;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;

    public UserController(CurrentUserService currentUserService, UserRepository userRepository) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<MessageRecipientOption>> lookup(@RequestParam String query) {
        User current = currentUserService.getCurrentUser();
        List<MessageRecipientOption> results = userRepository
                .findByEmployeeFullNameContainingIgnoreCase(query)
                .stream()
                .filter(u -> u.getEmployee() != null && !u.getId().equals(current.getId()))
                .map(MessageRecipientOption::from)
                .toList();
        return ResponseEntity.ok(results);
    }
}
