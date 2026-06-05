package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.LoginRequest;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.dtos.LogoutResponse;
import com.inholland.banking_app.security.JwtUtil;
import com.inholland.banking_app.security.TokenBlacklistService;
import com.inholland.banking_app.services.AuthService;
import jakarta.servlet.http.HttpServletRequest;
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
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        // Authenticates the user and returns a JWT token
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        log.info("User logged in: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        // Blacklists the current token so it cannot be reused after logout
        String token = parseToken(request);
        if (token != null && jwtUtil.validateJwtToken(token)) {
            tokenBlacklistService.blacklist(token, jwtUtil.getExpirationFromToken(token));
        }
        log.info("User logged out");
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully."));
    }

    private String parseToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
