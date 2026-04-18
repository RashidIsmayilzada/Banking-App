package com.inholland.banking_app.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = extractBearerToken(request);
        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isInvalidToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String username = jwtUtil.getUsernameFromToken(token);
        if (shouldSkipAuthentication(username)) {
            filterChain.doFilter(request, response);
            return;
        }

        User user = findUser(username);
        ErrorResponse errorResponse = validateUserAccess(user);
        if (errorResponse != null) {
            writeErrorResponse(response, errorResponse);
            return;
        }

        setAuthentication(request, username, user);
        filterChain.doFilter(request, response);
    }

    private String extractBearerToken(HttpServletRequest request) {
        String authorizationHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return null;
        }

        return authorizationHeader.substring(7);
    }

    private boolean isInvalidToken(String token) {
        return !jwtUtil.validateJwtToken(token);
    }

    private boolean shouldSkipAuthentication(String username) {
        return username == null || SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    private ErrorResponse validateUserAccess(User user) {
        if (user == null) {
            return unauthorizedUser();
        }

        if (!user.isActive()) {
            return inactiveUser();
        }

        if (user.getRole() == Role.CUSTOMER) {
            return validateCustomer(user.getCustomerProfile());
        }

        if (user.getRole() == Role.EMPLOYEE) {
            return validateEmployee(user.getEmployeeProfile());
        }

        return null;
    }

    private ErrorResponse validateCustomer(CustomerProfile profile) {
        if (profile == null) {
            return missingCustomerProfile();
        }

        if (profile.getStatus() != CustomerStatus.APPROVED) {
            return customerNotApproved();
        }

        return null;
    }

    private ErrorResponse validateEmployee(EmployeeProfile profile) {
        if (profile == null || !profile.isEnabled()) {
            return employeeNotEnabled();
        }

        return null;
    }

    private void setAuthentication(HttpServletRequest request, String username, User user) {
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                username,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private ErrorResponse unauthorizedUser() {
        return new ErrorResponse(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authenticated user not found");
    }

    private ErrorResponse inactiveUser() {
        return new ErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "User account is inactive");
    }

    private ErrorResponse missingCustomerProfile() {
        return new ErrorResponse(HttpStatus.NOT_FOUND, "NOT_FOUND", "Customer profile not found");
    }

    private ErrorResponse customerNotApproved() {
        return new ErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Customer account is not approved");
    }

    private ErrorResponse employeeNotEnabled() {
        return new ErrorResponse(HttpStatus.FORBIDDEN, "FORBIDDEN", "Employee account is not enabled");
    }

    private void writeErrorResponse(HttpServletResponse response, ErrorResponse errorResponse)
            throws IOException {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", errorResponse.code());
        body.put("message", errorResponse.message());
        body.put("timestamp", Instant.now().toString());

        response.setStatus(errorResponse.status().value());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private record ErrorResponse(HttpStatus status, String code, String message) {
    }
}
