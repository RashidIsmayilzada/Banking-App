package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.mappers.UserRequestMapper;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.UserFactory;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
    private final EmployeeProfileRepository employeeProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;
    private final UserRequestMapper userRequestMapper;
    private final UserResponseMapper userResponseMapper;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public LoginResponse login(String email, String password) {
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
    
    public UserResponse register(UserRequest request) {
        if (request.getRole() == null) {
            request.setRole(Role.CUSTOMER);
        }
        validateRegistrationRequest(request);
        request.setPassword(passwordEncoder.encode(request.getPassword()));
        request.setEmail(normalizeEmail(request.getEmail()));
        request.setUsername(request.getUsername().toLowerCase());

        User newUser = UserFactory.createUser(request);
        return userResponseMapper.toUserResponse(userRepository.save(newUser));
    }

    public AuthContextResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BadCredentialsException("Authenticated user not found"));

        return authMapper.toAuthContextResponse(user);
    }

    public void validateRegistrationRequest(UserRequest request) {
        validateUniqueEmail(request.getEmail());
        validateUniqueUsername(request.getUsername());
        validatePasswordStrength(request.getPassword());

        Role role = request.getRole() == null ? Role.CUSTOMER : request.getRole();
        if (role == Role.CUSTOMER) {
            validateUniqueBsn(request.getBsn());
        } else {
            validateUniqueEmployeeNumber(request.getEmployeeNumber());
        }
    }

    private void validateUniqueEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private void validateUniqueUsername(String username) {
        if (userRepository.existsByUsername(username)) {
            throw new DuplicateResourceException("Username already exists");
        }
    }

    private void validateUniqueBsn(String bsn) {
        if (customerProfileRepository.existsByBsn(bsn)) {
            throw new DuplicateResourceException("BSN already exists");
        }
    }

    private void validateUniqueEmployeeNumber(String employeeNumber) {
        if (employeeProfileRepository.existsByEmployeeNumber(employeeNumber)) {
            throw new DuplicateResourceException("Employee number already exists");
        }
    }

    private void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password does not meet strength requirements");
        }
        boolean hasUppercase = false, hasLowercase = false, hasDigit = false, hasSpecial = false;
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUppercase = true;
            else if (Character.isLowerCase(c)) hasLowercase = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }
        if (!hasUppercase || !hasLowercase || !hasDigit || !hasSpecial) {
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

    private void validateLoginAllowed(User user) {
        if (user.getRole() != Role.CUSTOMER) {
            return;
        }
        Optional<CustomerProfile> profile = customerProfileRepository.findById(user.getId());
        if (profile.isEmpty()) {
            return;
        }
        CustomerStatus status = profile.get().getStatus();
        if (status == CustomerStatus.REJECTED || status == CustomerStatus.CLOSED) {
            throw new ForbiddenException("This account is no longer allowed to access the application");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim();
    }
}
