package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.security.JwtUtil;
import com.inholland.banking_app.security.TokenBlacklistService;
import com.inholland.banking_app.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Authentication", description = "Login and logout endpoints")
@RequiredArgsConstructor
@RestController
@RequestMapping("/auth")
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;

    @Operation(summary = "Login to the application", description = "Returns an access token to be used in the Authorization header")
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request.getEmail(), request.getPassword());
        log.info("User logged in: {}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Logout from the application")
    @PostMapping("/logout")
    public ResponseEntity<LogoutResponse> logout(HttpServletRequest request) {
        String token = parseToken(request);
        if (token != null && jwtUtil.validateJwtToken(token)) {
            tokenBlacklistService.blacklist(token, jwtUtil.getExpirationFromToken(token));
        }
        log.info("User logged out");
        return ResponseEntity.ok(new LogoutResponse("Logged out successfully."));
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    private String parseToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        return (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
    }
}
