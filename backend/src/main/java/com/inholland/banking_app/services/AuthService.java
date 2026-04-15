package com.inholland.banking_app.services;

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

@RequiredArgsConstructor
@Service
public class AuthService {

    public final UserRepository userRepository;
    public final CustomerProfileRepository customerProfileRepository;
    public final PasswordEncoder passwordEncoder;
    public final JwtUtil jwtUtil;

    public void registerCustomerProfile(RegisterCustomerRequest request) {
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        if(userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if(customerProfileRepository.existsByBsn(request.getBsn())) {
            throw new IllegalArgumentException("BSN already exists");
        }

        if (!isPasswordStrong(request.getPassword())) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }

        String passwordHash = passwordEncoder.encode(request.getPassword());
        User user = UserFactory.createPendingCustomer(request, passwordHash);
        userRepository.save(user);
    }

    public String loginCustomer(String email, String password) {
        String normalizedEmail = email == null ? null : email.trim();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!validatePassword(password, user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isActive()) {
            throw new DisabledException("User account is inactive");
        }

        if (user.getRole() == Role.CUSTOMER && user.getCustomerProfile() != null) {
            CustomerStatus status = user.getCustomerProfile().getStatus();
            if (status != CustomerStatus.APPROVED) {
                throw new LockedException("Customer account is not approved");
            }
        }

        return jwtUtil.generateToken(user.getUsername());
    }

    private Boolean validatePassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    // Password Strength Validation
    public boolean isPasswordStrong(String password) {
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
