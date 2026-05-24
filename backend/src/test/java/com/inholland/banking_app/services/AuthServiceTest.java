package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuthContextResponse;
import com.inholland.banking_app.dtos.LoginResponse;
import com.inholland.banking_app.exceptions.ForbiddenException;
import com.inholland.banking_app.mappers.AuthMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthMapper authMapper;

    @InjectMocks private AuthService authService;

    private User activeEmployee;
    private User activeCustomer;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "jwtExpirationMs", 3600000L);

        activeEmployee = new User();
        activeEmployee.setId(1L);
        activeEmployee.setEmail("employee@bank.com");
        activeEmployee.setUsername("employee");
        activeEmployee.setPasswordHash("hashed");
        activeEmployee.setRole(Role.EMPLOYEE);
        activeEmployee.setActive(true);
        activeEmployee.setCreatedAt(LocalDateTime.now());
        activeEmployee.setUpdatedAt(LocalDateTime.now());

        activeCustomer = new User();
        activeCustomer.setId(2L);
        activeCustomer.setEmail("customer@bank.com");
        activeCustomer.setUsername("customer");
        activeCustomer.setPasswordHash("hashed");
        activeCustomer.setRole(Role.CUSTOMER);
        activeCustomer.setActive(true);
        activeCustomer.setCreatedAt(LocalDateTime.now());
        activeCustomer.setUpdatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("login() - should return LoginResponse when credentials are valid")
    void login_shouldReturnLoginResponse_whenCredentialsAreValid() {
        AuthContextResponse context = new AuthContextResponse();
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(activeEmployee));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("employee")).thenReturn("jwt-token");
        when(authMapper.toAuthContextResponse(activeEmployee)).thenReturn(context);
        when(userRepository.save(any(User.class))).thenReturn(activeEmployee);

        LoginResponse response = authService.login("employee@bank.com", "password");

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(3600);
        assertThat(response.getUser()).isSameAs(context);
        verify(userRepository).save(activeEmployee);
    }

    @Test
    @DisplayName("login() - should trim whitespace from email before lookup")
    void login_shouldNormalizeEmail_byTrimmingWhitespace() {
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(activeEmployee));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(jwtUtil.generateToken("employee")).thenReturn("token");
        when(authMapper.toAuthContextResponse(any())).thenReturn(new AuthContextResponse());
        when(userRepository.save(any())).thenReturn(activeEmployee);

        authService.login("  employee@bank.com  ", "password");

        verify(userRepository).findByEmail("employee@bank.com");
    }

    @Test
    @DisplayName("login() - should throw BadCredentialsException when user not found")
    void login_shouldThrowBadCredentialsException_whenUserNotFound() {
        when(userRepository.findByEmail("unknown@bank.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("unknown@bank.com", "password"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login() - should throw BadCredentialsException when password is wrong")
    void login_shouldThrowBadCredentialsException_whenPasswordIsWrong() {
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(activeEmployee));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("employee@bank.com", "wrong"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("login() - should throw DisabledException when user account is inactive")
    void login_shouldThrowDisabledException_whenUserIsInactive() {
        activeEmployee.setActive(false);
        when(userRepository.findByEmail("employee@bank.com")).thenReturn(Optional.of(activeEmployee));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("employee@bank.com", "password"))
                .isInstanceOf(DisabledException.class)
                .hasMessageContaining("inactive");
    }

    @Test
    @DisplayName("login() - should throw ForbiddenException when customer status is REJECTED")
    void login_shouldThrowForbiddenException_whenCustomerStatusIsRejected() {
        CustomerProfile profile = new CustomerProfile();
        profile.setStatus(CustomerStatus.REJECTED);

        when(userRepository.findByEmail("customer@bank.com")).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(customerProfileRepository.findById(2L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> authService.login("customer@bank.com", "password"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("no longer allowed");
    }

    @Test
    @DisplayName("login() - should throw ForbiddenException when customer status is CLOSED")
    void login_shouldThrowForbiddenException_whenCustomerStatusIsClosed() {
        CustomerProfile profile = new CustomerProfile();
        profile.setStatus(CustomerStatus.CLOSED);

        when(userRepository.findByEmail("customer@bank.com")).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(customerProfileRepository.findById(2L)).thenReturn(Optional.of(profile));

        assertThatThrownBy(() -> authService.login("customer@bank.com", "password"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("login() - should succeed when customer status is APPROVED")
    void login_shouldSucceed_whenCustomerHasApprovedStatus() {
        CustomerProfile profile = new CustomerProfile();
        profile.setStatus(CustomerStatus.APPROVED);

        when(userRepository.findByEmail("customer@bank.com")).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(customerProfileRepository.findById(2L)).thenReturn(Optional.of(profile));
        when(jwtUtil.generateToken("customer")).thenReturn("token");
        when(authMapper.toAuthContextResponse(activeCustomer)).thenReturn(new AuthContextResponse());
        when(userRepository.save(any())).thenReturn(activeCustomer);

        LoginResponse response = authService.login("customer@bank.com", "password");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("login() - should succeed when customer has no profile yet")
    void login_shouldSucceed_whenCustomerHasNoProfile() {
        when(userRepository.findByEmail("customer@bank.com")).thenReturn(Optional.of(activeCustomer));
        when(passwordEncoder.matches("password", "hashed")).thenReturn(true);
        when(customerProfileRepository.findById(2L)).thenReturn(Optional.empty());
        when(jwtUtil.generateToken("customer")).thenReturn("token");
        when(authMapper.toAuthContextResponse(activeCustomer)).thenReturn(new AuthContextResponse());
        when(userRepository.save(any())).thenReturn(activeCustomer);

        LoginResponse response = authService.login("customer@bank.com", "password");

        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("getCurrentUser() - should return AuthContextResponse when user exists")
    void getCurrentUser_shouldReturnAuthContextResponse_whenUserExists() {
        AuthContextResponse expected = new AuthContextResponse();
        when(userRepository.findByUsername("employee")).thenReturn(Optional.of(activeEmployee));
        when(authMapper.toAuthContextResponse(activeEmployee)).thenReturn(expected);

        AuthContextResponse result = authService.getCurrentUser("employee");

        assertThat(result).isSameAs(expected);
    }

    @Test
    @DisplayName("getCurrentUser() - should throw BadCredentialsException when user not found")
    void getCurrentUser_shouldThrowBadCredentialsException_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("unknown"))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("not found");
    }
}
