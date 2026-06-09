package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.mappers.UserRequestMapper;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.factory.UserFactory;
import com.inholland.banking_app.policies.UserPolicy;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@RequiredArgsConstructor
@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Invalid email or password";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthMapper authMapper;
    private final UserRequestMapper userRequestMapper;
    private final UserResponseMapper userResponseMapper;
    private final UserPolicy userPolicy;

    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    public LoginResponse login(String email, String password) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new BadCredentialsException(INVALID_CREDENTIALS_MESSAGE));

        userPolicy.assertPasswordMatches(password, user);
        userPolicy.assertActiveUser(user);
        userPolicy.assertLoginAllowed(user);

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
        userPolicy.assertRegistrationRequest(request);
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

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim();
    }
}