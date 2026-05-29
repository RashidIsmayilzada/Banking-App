package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public LoginResponse login(String email, String password) {
        // Validates credentials, checks account status, records last login, and returns a JWT response
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        validatePassword(password, user);
        validateActiveUser(user);
        validateLoginAllowed(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = jwtUtil.generateToken(user.getUsername());
        AuthContextResponse authContext = authMapper.toAuthContextResponse(user);

        return new LoginResponse(token, "Bearer", (int) (jwtExpirationMs / 1000), authContext);
    }

    public AuthContextResponse getCurrentUser(String username) {
        // Looks up the authenticated user by username and returns their context data
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        return authMapper.toAuthContextResponse(user);
    }

    private void validatePassword(String password, User user) {
        // Throws BadCredentialsException if the provided password does not match the stored hash
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private void validateActiveUser(User user) {
        // Throws DisabledException if the user account is marked as inactive
        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }
    }

    private void validateLoginAllowed(User user) {
        // Blocks login for customers whose profile is marked as login-blocked; employees always pass
        if (user.getRole() != Role.CUSTOMER) {
            return;
        }
        Optional<CustomerProfile> profile = customerProfileRepository.findById(user.getId());
        if (profile.isEmpty()) {
            return;
        }
        if (profile.get().isLoginBlocked()) {
            throw new ForbiddenException("This account is no longer allowed to access the application");
        }
    }

    private String normalizeEmail(String email) {
        // Trims whitespace from the email address before repository lookup
        return email == null ? null : email.trim();
    }
}
