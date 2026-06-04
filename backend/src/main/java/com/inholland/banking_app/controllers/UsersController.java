package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.UserCreateRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/users")
@Slf4j
public class UsersController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserResponse> createUser(@Valid @RequestBody UserCreateRequest request) {
        // Creates a new user account and returns the created user
        UserResponse response = userService.createUser(request);
        log.info("User created: {} (role={})", response.getEmail(), response.getRole());
        return ResponseEntity.status(201).body(response);
    }
}
