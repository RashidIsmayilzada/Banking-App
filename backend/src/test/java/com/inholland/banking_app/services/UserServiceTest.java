package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.ApproveCustomerRequest;
import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.mappers.UserResponseMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AccountService accountService;
    @Mock private TransactionRepository transactionRepository;
    @Mock private UserResponseMapper userResponseMapper;

    // UserService is tested in isolation: every collaborator (including AccountService)
    // is a mock, so a failure here can only be a bug in UserService itself.
    @InjectMocks private UserService userService;

    private User customerUser;
    private CustomerProfile customerProfile;
    private UserResponse userResponse;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        customerUser = new User();
        customerUser.setId(1L);
        customerUser.setEmail("john@example.com");
        customerUser.setUsername("john_doe");
        customerUser.setRole(Role.CUSTOMER);
        customerUser.setActive(true);
        customerUser.setCreatedAt(LocalDateTime.now());
        customerUser.setUpdatedAt(LocalDateTime.now());
        customerUser.setAccounts(new ArrayList<>());

        customerProfile = new CustomerProfile();
        customerProfile.setUserId(1L);
        customerProfile.setUser(customerUser);
        customerProfile.setFirstName("John");
        customerProfile.setLastName("Doe");
        customerProfile.setBsn("123456789");
        customerProfile.setPhoneNumber("+31612345678");
        customerProfile.setStatus(CustomerStatus.PENDING_APPROVAL);
        customerProfile.setRegisteredAt(LocalDateTime.now());

        customerUser.setCustomerProfile(customerProfile);

        userResponse = UserResponse.builder()
                .id(1L)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .username("john_doe")
                .role(Role.CUSTOMER)
                .status(CustomerStatus.PENDING_APPROVAL)
                .hasAccounts(false)
                .accountCount(0)
                .build();

        pageable = PageRequest.of(0, 10);
    }

    // --- getAllUsers ---

    @Test
    @DisplayName("getAllUsers() - should return paginated user responses")
    void getAllUsers_shouldReturnPage_whenUsersExist() {
        Page<User> userPage = new PageImpl<>(List.of(customerUser), pageable, 1);
        UserFilterRequest filter = new UserFilterRequest();

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(userPage);
        when(userResponseMapper.toUserResponse(customerUser)).thenReturn(userResponse);

        Page<UserResponse> result = userService.getAllUsers(pageable, filter);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getEmail()).isEqualTo("john@example.com");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(userRepository).findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable));
    }

    @Test
    @DisplayName("getAllUsers() - should return empty page when no users match filter")
    void getAllUsers_shouldReturnEmptyPage_whenNoUsersMatch() {
        Page<User> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        UserFilterRequest filter = new UserFilterRequest();

        when(userRepository.findAll(ArgumentMatchers.<Specification<User>>any(), eq(pageable))).thenReturn(emptyPage);

        Page<UserResponse> result = userService.getAllUsers(pageable, filter);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    // --- getUserById ---

    @Test
    @DisplayName("getUserById() - should return user response when user exists")
    void getUserById_shouldReturnResponse_whenUserExists() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(userResponseMapper.toUserResponse(customerUser)).thenReturn(userResponse);

        UserResponse result = userService.getUserById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("getUserById() - should throw EntityNotFoundException when user not found")
    void getUserById_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User with id 99 not found");
    }

    // --- approveCustomer ---

    @Test
    @DisplayName("approveCustomer() - should set status APPROVED, activate user and delegate account creation")
    void approveCustomer_shouldApproveAndCreateAccounts_whenStatusIsApproved() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.APPROVED);
        customerUser.setActive(false); // a pending customer is inactive until approved

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(accountService.hasNoAccounts(customerUser)).thenReturn(true);

        userService.approveCustomer(request, 1L);

        assertThat(customerProfile.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        assertThat(customerUser.isActive()).isTrue();
        verify(accountService).createDefaultAccounts(customerUser, null, null, null);
    }

    @Test
    @DisplayName("approveCustomer() - should set status REJECTED without creating accounts")
    void approveCustomer_shouldReject_withoutCreatingAccounts() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.REJECTED);

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        userService.approveCustomer(request, 1L);

        assertThat(customerProfile.getStatus()).isEqualTo(CustomerStatus.REJECTED);
        verify(accountService, never()).createDefaultAccounts(any(), any(), any(), any());
    }

    @Test
    @DisplayName("approveCustomer() - should throw EntityNotFoundException when user not found")
    void approveCustomer_shouldThrow_whenUserNotFound() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.APPROVED);

        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.approveCustomer(request, 99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("approveCustomer() - should throw EntityNotFoundException when customer profile is null")
    void approveCustomer_shouldThrow_whenCustomerProfileIsNull() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.APPROVED);

        customerUser.setCustomerProfile(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> userService.approveCustomer(request, 1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Customer profile not found");
    }

    @Test
    @DisplayName("approveCustomer() - should pass the provided absolute + daily limits to account creation")
    void approveCustomer_shouldApplyCustomLimits_whenLimitsProvided() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.APPROVED);
        request.setCheckingAbsoluteLimit(new BigDecimal("-500.00"));
        request.setCheckingDailyLimit(new BigDecimal("500.00"));
        request.setSavingsDailyLimit(new BigDecimal("3000.00"));

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(accountService.hasNoAccounts(customerUser)).thenReturn(true);

        userService.approveCustomer(request, 1L);

        verify(accountService).createDefaultAccounts(customerUser,
                new BigDecimal("-500.00"), new BigDecimal("500.00"), new BigDecimal("3000.00"));
    }

    @Test
    @DisplayName("approveCustomer() - should not create accounts when customer is already approved")
    void approveCustomer_shouldNotCreateAccounts_whenAlreadyApproved() {
        ApproveCustomerRequest request = new ApproveCustomerRequest();
        request.setStatus(CustomerStatus.APPROVED);

        customerProfile.setStatus(CustomerStatus.APPROVED);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        userService.approveCustomer(request, 1L);

        verify(accountService, never()).createDefaultAccounts(any(), any(), any(), any());
    }

    // --- getByUsername ---

    @Test
    @DisplayName("getByUsername() - should return user when found")
    void getByUsername_shouldReturnUser_whenFound() {
        when(userRepository.findByUsername("john_doe")).thenReturn(Optional.of(customerUser));

        User result = userService.getByUsername("john_doe");

        assertThat(result.getUsername()).isEqualTo("john_doe");
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getByUsername() - should throw EntityNotFoundException when user not found")
    void getByUsername_shouldThrow_whenUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getByUsername("unknown"))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User not found: unknown");
    }

    // --- closeUser ---

    @Test
    @DisplayName("closeUser() - should set profile CLOSED and delegate account closing for a customer")
    void closeUser_shouldCloseProfileAndAccounts_whenCustomer() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(userResponseMapper.toUserResponse(customerUser)).thenReturn(userResponse);

        userService.closeUser(1L);

        assertThat(customerProfile.getStatus()).isEqualTo(CustomerStatus.CLOSED);
        verify(accountService).closeAllAccounts(customerUser);
    }

    @Test
    @DisplayName("closeUser() - should set active=false for an employee and not touch accounts")
    void closeUser_shouldSetInactive_whenEmployee() {
        User employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setActive(true);

        UserResponse employeeResponse = UserResponse.builder()
                .id(2L).role(Role.EMPLOYEE).active(false).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(userResponseMapper.toUserResponse(employeeUser)).thenReturn(employeeResponse);

        userService.closeUser(2L);

        assertThat(employeeUser.isActive()).isFalse();
        verify(userRepository).save(employeeUser);
        verify(accountService, never()).closeAllAccounts(any());
    }

    @Test
    @DisplayName("closeUser() - should throw EntityNotFoundException when user not found")
    void closeUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.closeUser(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User with id 99 not found");
    }

    @Test
    @DisplayName("closeUser() - should throw EntityNotFoundException when customer has no profile")
    void closeUser_shouldThrow_whenCustomerProfileIsNull() {
        customerUser.setCustomerProfile(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> userService.closeUser(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Customer profile not found");
    }

    // --- reopenUser ---

    @Test
    @DisplayName("reopenUser() - should set profile APPROVED and delegate account reopening for a customer")
    void reopenUser_shouldApproveProfileAndReopenAccounts_whenCustomer() {
        customerProfile.setStatus(CustomerStatus.CLOSED);

        UserResponse reopenedResponse = UserResponse.builder()
                .id(1L).role(Role.CUSTOMER).status(CustomerStatus.APPROVED).build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));
        when(userResponseMapper.toUserResponse(customerUser)).thenReturn(reopenedResponse);

        userService.reopenUser(1L);

        assertThat(customerProfile.getStatus()).isEqualTo(CustomerStatus.APPROVED);
        verify(accountService).reopenAllAccounts(customerUser);
    }

    @Test
    @DisplayName("reopenUser() - should set active=true for an employee and not touch accounts")
    void reopenUser_shouldSetActive_whenEmployee() {
        User employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setRole(Role.EMPLOYEE);
        employeeUser.setActive(false);

        UserResponse employeeResponse = UserResponse.builder()
                .id(2L).role(Role.EMPLOYEE).active(true).build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(userResponseMapper.toUserResponse(employeeUser)).thenReturn(employeeResponse);

        userService.reopenUser(2L);

        assertThat(employeeUser.isActive()).isTrue();
        verify(userRepository).save(employeeUser);
        verify(accountService, never()).reopenAllAccounts(any());
    }

    @Test
    @DisplayName("reopenUser() - should throw EntityNotFoundException when user not found")
    void reopenUser_shouldThrow_whenUserNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.reopenUser(99L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("User with id 99 not found");
    }

    @Test
    @DisplayName("reopenUser() - should throw EntityNotFoundException when customer has no profile")
    void reopenUser_shouldThrow_whenCustomerProfileIsNull() {
        customerUser.setCustomerProfile(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(customerUser));

        assertThatThrownBy(() -> userService.reopenUser(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessageContaining("Customer profile not found");
    }
}
