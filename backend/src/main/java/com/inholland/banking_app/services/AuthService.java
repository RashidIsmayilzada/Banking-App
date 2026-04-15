package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.dtos.RegisterCustomerRequest;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.UserFactory;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    public final UserRepository userRepository;
    public final CustomerProfileRepository customerProfileRepository;
    public final PasswordEncoder passwordEncoder;
    public final JwtUtil jwtUtil;
    public final AuthMapper authMapper;

    public void registerCustomerProfile(RegisterCustomerRequest request) {
        validateRegistrationRequest(request);

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = UserFactory.createPendingCustomer(request, passwordHash);
        userRepository.save(user);
    }

    public String loginCustomer(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        validatePassword(password, user);
        validateActiveUser(user);
        validateApprovedCustomer(user);

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return jwtUtil.generateToken(user.getUsername());
    }

    public AuthContextResponse getCurrentCustomer(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        return authMapper.toAuthContextResponse(user);
    }


    // HELPER METHODS

    // Validation orchestator
    private void validateRegistrationRequest(RegisterCustomerRequest request) {
        validateUniqueEmail(request.getEmail());
        validateUniqueUsername(request.getUsername());
        validateUniqueBsn(request.getBsn());
        validatePasswordStrength(request.getPassword());
    }

    // Individual validation methods
    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
    }

    private void validateUniqueBsn(String bsn) {
        if (customerProfileRepository.existsByBsn(bsn)) {
            throw new IllegalArgumentException("BSN already exists");
        }
    }

    private void validatePasswordStrength(String password) {
        if (!isPasswordStrong(password)) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }
    }

    private void validatePassword(String password, User user) {
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE);
        }
    }

    private void validateActiveUser(User user) {
        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }
    }

    private void validateApprovedCustomer(User user) {
        if (user.getRole() != Role.CUSTOMER || user.getCustomerProfile() == null) {
            return;
        }

        CustomerStatus status = user.getCustomerProfile().getStatus();
        if (status != CustomerStatus.APPROVED) {
            throw new LockedException("Customer account is not approved");
        }
    }

    // Utility methods
    private String normalizeEmail(String email) {
        return email == null ? null : email.trim();
    }

    private boolean isPasswordStrong(String password) {
        if (password == null || password.length() < 8) {
            return false;
        }
        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(c)) {
                hasLowercase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }
        return hasUppercase && hasLowercase && hasDigit && hasSpecialChar;
    }
}
