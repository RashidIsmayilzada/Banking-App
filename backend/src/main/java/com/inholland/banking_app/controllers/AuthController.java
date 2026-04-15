package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.LoginRequest;
import com.inholland.banking_app.dtos.RegisterCustomerRequest;
import com.inholland.banking_app.services.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/register/customer")
    public ResponseEntity<String> registerCustomerProfile(@RequestBody RegisterCustomerRequest request) {
        authService.registerCustomerProfile(request);
        log.info("Customer profile registered successfully: {}", request.getEmail());
        return ResponseEntity.status(201)
                .body("Customer profile registered successfully");
    }

    @PostMapping("/auth/login")
    public ResponseEntity<String> loginCustomer(@RequestBody LoginRequest request) {
        String token = authService.loginCustomer(request.getEmail(), request.getPassword());
        log.info("Customer logged in successfully: {}", request.getEmail());
        // This log statement is for testing. After we finish development I will remove this
        // TODO: Remove this log statement after development is complete
        log.info("Generated JWT token: {}", token);
        return ResponseEntity.ok(token);
    }
}
