package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.LoginRequest;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.dtos.LogoutResponse;
import com.inholland.banking_app.services.AuthService;
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
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        log.info("User logged in: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout() {
        log.info("User logged out");
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully."));
    }
}
