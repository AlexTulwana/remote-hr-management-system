package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.CurrentUserResponse;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;

    public UserController(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }
}
