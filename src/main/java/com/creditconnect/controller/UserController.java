package com.creditconnect.controller;

import com.creditconnect.dto.CreateUserRequest;
import com.creditconnect.dto.LoanResponse;
import com.creditconnect.dto.UserResponse;
import com.creditconnect.service.RepaymentService;
import com.creditconnect.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RepaymentService repaymentService;

    public UserController(UserService userService, RepaymentService repaymentService) {
        this.userService = userService;
        this.repaymentService = repaymentService;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(request));
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable Long id) {
        return userService.getById(id);
    }

    @GetMapping("/{id}/loans")
    public List<LoanResponse> loans(@PathVariable Long id) {
        userService.getById(id);
        return repaymentService.loansForUser(id);
    }
}
