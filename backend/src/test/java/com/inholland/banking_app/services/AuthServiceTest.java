package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.dtos.UserRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.mappers.UserRequestMapper;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.policies.UserPolicy;
import com.inholland.banking_app.repositories.UserRepository;
import com.inholland.banking_app.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthMapper authMapper;
    @Mock private UserRequestMapper userRequestMapper;
    @Mock private UserResponseMapper userResponseMapper;
    @Mock private UserPolicy userPolicy;

    // BCryptPasswordEncoder is used in AuthService only to encode on register, not inject into JwtUtil
    @org.mockito.Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private User activeUser;
    private AuthContextResponse authContext;

    @BeforeEach
    void setUp() {
        // Inject the JWT expiration value that is normally read from application properties
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);

        activeUser = new User();
        activeUser.setId(1L);
        activeUser.setEmail("john@example.com");
        activeUser.setUsername("john");
        activeUser.setPasswordHash("$2a$hashedpassword");
        activeUser.setRole(Role.CUSTOMER);
        activeUser.setActive(true);
        activeUser.setCreatedAt(LocalDateTime.now());
        activeUser.setUpdatedAt(LocalDateTime.now());

        authContext = new AuthContextResponse();
        authContext.setUserId(1L);
        authContext.setEmail("john@example.com");
        authContext.setUsername("john");
        authContext.setRole(Role.CUSTOMER);
        authContext.setActive(true);
    }

    // login() tests

    @Test
    @DisplayName("login() - should return a LoginResponse with a token when credentials are valid")
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateToken("john")).thenReturn("mocked-jwt-token");
        when(authMapper.toAuthContextResponse(activeUser)).thenReturn(authContext);
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        LoginResponse result = authService.login("john@example.com", "Secret@123");

        assertThat(result.getAccessToken()).isEqualTo("mocked-jwt-token");
        assertThat(result.getTokenType()).isEqualTo("Bearer");
        assertThat(result.getExpiresIn()).isEqualTo(3600);
        assertThat(result.getUser().getUsername()).isEqualTo("john");
    }

    @Test
    @DisplayName("login() - should update lastLoginAt on the user when login succeeds")
    void login_shouldUpdateLastLoginAt_onSuccess() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateToken("john")).thenReturn("mocked-jwt-token");
        when(authMapper.toAuthContextResponse(activeUser)).thenReturn(authContext);
        when(userRepository.save(activeUser)).thenReturn(activeUser);

        authService.login("john@example.com", "Secret@123");

        // lastLoginAt must be set before saving so subsequent logins show the correct time
        assertThat(activeUser.getLastLoginAt()).isNotNull();
        verify(userRepository).save(activeUser);
    }

    @Test
    @DisplayName("login() - should trim leading and trailing whitespace from the email before lookup")
    void login_shouldNormalizeEmail_byTrimming() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(jwtUtil.generateToken("john")).thenReturn("token");
        when(authMapper.toAuthContextResponse(activeUser)).thenReturn(authContext);

        authService.login("  john@example.com  ", "Secret@123");

        verify(userRepository).findByEmail("john@example.com");
    }

    @Test
    @DisplayName("login() - should throw BadCredentialsException when the email is not registered")
    void login_shouldThrowBadCredentials_whenEmailNotFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown@example.com", "any"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Invalid email or password");
    }

    @Test
    @DisplayName("login() - should propagate BadCredentialsException when the password is wrong")
    void login_shouldPropagateException_whenPasswordIsWrong() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        doThrow(new BadCredentialsException("Invalid email or password"))
                .when(userPolicy).assertPasswordMatches(anyString(), eq(activeUser));

        assertThatThrownBy(() -> authService.login("john@example.com", "WrongPass"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login() - should propagate DisabledException when the user account is inactive")
    void login_shouldPropagateException_whenUserIsInactive() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        doThrow(new DisabledException("User account is inactive"))
                .when(userPolicy).assertActiveUser(activeUser);

        assertThatThrownBy(() -> authService.login("john@example.com", "Secret@123"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("login() - should propagate ForbiddenException when the customer account is rejected or closed")
    void login_shouldPropagateException_whenLoginNotAllowed() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        doThrow(new com.inholland.banking_app.exceptions.ForbiddenException("account is no longer allowed"))
                .when(userPolicy).assertLoginAllowed(activeUser);

        assertThatThrownBy(() -> authService.login("john@example.com", "Secret@123"))
                .isInstanceOf(com.inholland.banking_app.exceptions.ForbiddenException.class);
    }

    // register() tests

    @Test
    @DisplayName("register() - should default the role to CUSTOMER when no role is provided in the request")
    void register_shouldDefaultRoleToCustomer_whenRoleIsNull() {
        UserRequest request = buildCustomerRequest();
        request.setRole(null);

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setRole(Role.CUSTOMER);

        UserResponse expectedResponse = new UserResponse();

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userResponseMapper.toUserResponse(savedUser)).thenReturn(expectedResponse);

        UserResponse result = authService.register(request);

        assertThat(request.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(result).isSameAs(expectedResponse);
    }

    @Test
    @DisplayName("register() - should lowercase the username and trim the email before saving")
    void register_shouldNormalizeUsernameAndEmail_beforeSaving() {
        UserRequest request = buildCustomerRequest();
        request.setUsername("JohnDoe");
        request.setEmail("  John@Example.com  ");

        User savedUser = new User();
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userResponseMapper.toUserResponse(savedUser)).thenReturn(new UserResponse());

        authService.register(request);

        assertThat(request.getUsername()).isEqualTo("johndoe");
        assertThat(request.getEmail()).isEqualTo("John@Example.com");
    }

    @Test
    @DisplayName("register() - should encode the raw password before creating the user entity")
    void register_shouldEncodePassword_beforeSaving() {
        UserRequest request = buildCustomerRequest();

        User savedUser = new User();
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userResponseMapper.toUserResponse(savedUser)).thenReturn(new UserResponse());

        authService.register(request);

        assertThat(request.getPassword()).isEqualTo("$2a$encoded");
        verify(passwordEncoder).encode("Secret@123");
    }

    @Test
    @DisplayName("register() - should call assertRegistrationRequest on the policy to validate the request")
    void register_shouldCallPolicyValidation() {
        UserRequest request = buildCustomerRequest();

        User savedUser = new User();
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$encoded");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(userResponseMapper.toUserResponse(savedUser)).thenReturn(new UserResponse());

        authService.register(request);

        verify(userPolicy).assertRegistrationRequest(request);
    }

    @Test
    @DisplayName("register() - should propagate DuplicateResourceException when the email is already in use")
    void register_shouldPropagateException_whenEmailAlreadyExists() {
        UserRequest request = buildCustomerRequest();
        doThrow(new DuplicateResourceException("Email already exists"))
                .when(userPolicy).assertRegistrationRequest(request);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email already exists");
    }

    // getCurrentUser() tests

    @Test
    @DisplayName("getCurrentUser() - should return AuthContextResponse for an authenticated username")
    void getCurrentUser_shouldReturnAuthContext_whenUserExists() {
        when(userRepository.findByUsername("john")).thenReturn(Optional.of(activeUser));
        when(authMapper.toAuthContextResponse(activeUser)).thenReturn(authContext);

        AuthContextResponse result = authService.getCurrentUser("john");

        assertThat(result.getUsername()).isEqualTo("john");
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("getCurrentUser() - should throw BadCredentialsException when the username is not found")
    void getCurrentUser_shouldThrow_whenUsernameNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("ghost"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("Authenticated user not found");
    }

    // helpers

    private UserRequest buildCustomerRequest() {
        UserRequest request = new UserRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@example.com");
        request.setUsername("john");
        request.setPassword("Secret@123");
        request.setRole(Role.CUSTOMER);
        request.setBsn("123456789");
        request.setPhoneNumber("+31612345678");
        return request;
    }
}
