package com.wethinkcode.hrsystem.controller;

import com.wethinkcode.hrsystem.dto.CurrentUserResponse;
import com.wethinkcode.hrsystem.dto.MessageRecipientOption;
import com.wethinkcode.hrsystem.dto.UpdateContactRequest;
import com.wethinkcode.hrsystem.model.User;
import com.wethinkcode.hrsystem.repository.UserRepository;
import com.wethinkcode.hrsystem.security.CurrentUserService;
import com.wethinkcode.hrsystem.service.EmployeeService;
import com.wethinkcode.hrsystem.service.MessageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserRepository userRepository;
    private final EmployeeService employeeService;
    private final MessageService messageService;

    public UserController(CurrentUserService currentUserService, UserRepository userRepository,
                          EmployeeService employeeService, MessageService messageService) {
        this.currentUserService = currentUserService;
        this.userRepository = userRepository;
        this.employeeService = employeeService;
        this.messageService = messageService;
    }

    @GetMapping("/me")
    public ResponseEntity<CurrentUserResponse> me() {
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }

    @PutMapping("/me/contact")
    public ResponseEntity<CurrentUserResponse> updateMyContact(@RequestBody UpdateContactRequest request) {
        User current = currentUserService.getCurrentUser();
        if (current.getEmployee() == null) {
            throw new RuntimeException("No employee record is linked to this account");
        }
        employeeService.updateOwnContact(current.getEmployee().getId(), request);
        return ResponseEntity.ok(CurrentUserResponse.from(currentUserService.getCurrentUser()));
    }

    @GetMapping("/lookup")
    public ResponseEntity<List<MessageRecipientOption>> lookup(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long branchId) {
        List<MessageRecipientOption> results = messageService.lookupRecipients(query, branchId)
                .stream()
                .map(MessageRecipientOption::from)
                .toList();
        return ResponseEntity.ok(results);
    }
}
