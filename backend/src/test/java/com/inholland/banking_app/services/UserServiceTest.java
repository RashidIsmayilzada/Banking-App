package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.UserCreateRequest;
import com.inholland.banking_app.dtos.UserResponse;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.mappers.UserMapper;
import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private CustomerProfileRepository customerProfileRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private UserMapper userMapper;

    @InjectMocks private UserService userService;

    private UserCreateRequest customerRequest;
    private UserCreateRequest employeeRequest;

    @BeforeEach
    void setUp() {
        customerRequest = new UserCreateRequest();
        customerRequest.setFirstName("Jane");
        customerRequest.setLastName("Doe");
        customerRequest.setEmail("jane@bank.com");
        customerRequest.setUsername("janedoe");
        customerRequest.setPassword("securePassword123");
        customerRequest.setBsn("123456789");
        customerRequest.setPhoneNumber("0612345678");
        customerRequest.setRole(Role.CUSTOMER);

        employeeRequest = new UserCreateRequest();
        employeeRequest.setFirstName("John");
        employeeRequest.setLastName("Smith");
        employeeRequest.setEmail("john@bank.com");
        employeeRequest.setUsername("johnsmith");
        employeeRequest.setPassword("securePassword123");
        employeeRequest.setBsn("987654321");
        employeeRequest.setPhoneNumber("0698765432");
        employeeRequest.setRole(Role.EMPLOYEE);
    }

    @Test
    @DisplayName("createUser() - should create customer and return response when request is valid")
    void createUser_shouldCreateCustomer_whenValidCustomerRequest() {
        UserResponse expectedResponse = UserResponse.builder()
                .id(1L).email("jane@bank.com").role(Role.CUSTOMER)
                .status(CustomerStatus.PENDING_APPROVAL).build();

        when(userRepository.existsByEmail("jane@bank.com")).thenReturn(false);
        when(userRepository.existsByUsername("janedoe")).thenReturn(false);
        when(customerProfileRepository.existsByBsn("123456789")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(1L);
            return u;
        });
        when(customerProfileRepository.save(any(CustomerProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toCustomerResponse(any(User.class), any(CustomerProfile.class))).thenReturn(expectedResponse);

        UserResponse result = userService.createUser(customerRequest);

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("jane@bank.com");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
        verify(userRepository).save(any(User.class));
        verify(customerProfileRepository).save(any(CustomerProfile.class));
        verify(employeeProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser() - should create employee and return response when request is valid")
    void createUser_shouldCreateEmployee_whenValidEmployeeRequest() {
        UserResponse expectedResponse = UserResponse.builder()
                .id(2L).email("john@bank.com").role(Role.EMPLOYEE).build();

        when(userRepository.existsByEmail("john@bank.com")).thenReturn(false);
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(passwordEncoder.encode("securePassword123")).thenReturn("hashed");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(2L);
            return u;
        });
        when(employeeProfileRepository.save(any(EmployeeProfile.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toEmployeeResponse(any(User.class), any(EmployeeProfile.class))).thenReturn(expectedResponse);

        UserResponse result = userService.createUser(employeeRequest);

        assertThat(result).isNotNull();
        assertThat(result.getRole()).isEqualTo(Role.EMPLOYEE);
        verify(employeeProfileRepository).save(any(EmployeeProfile.class));
        verify(customerProfileRepository, never()).save(any());
    }

    @Test
    @DisplayName("createUser() - should throw DuplicateResourceException when email already exists")
    void createUser_shouldThrowDuplicateResourceException_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("jane@bank.com")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(customerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Email");
    }

    @Test
    @DisplayName("createUser() - should throw DuplicateResourceException when username already exists")
    void createUser_shouldThrowDuplicateResourceException_whenUsernameAlreadyExists() {
        when(userRepository.existsByEmail("jane@bank.com")).thenReturn(false);
        when(userRepository.existsByUsername("janedoe")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(customerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Username");
    }

    @Test
    @DisplayName("createUser() - should throw DuplicateResourceException when BSN already exists for customer")
    void createUser_shouldThrowDuplicateResourceException_whenBsnAlreadyExistsForCustomer() {
        when(userRepository.existsByEmail("jane@bank.com")).thenReturn(false);
        when(userRepository.existsByUsername("janedoe")).thenReturn(false);
        when(customerProfileRepository.existsByBsn("123456789")).thenReturn(true);

        assertThatThrownBy(() -> userService.createUser(customerRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("BSN");
    }

    @Test
    @DisplayName("createUser() - should skip BSN uniqueness check when role is EMPLOYEE")
    void createUser_shouldNotValidateBsn_whenRoleIsEmployee() {
        when(userRepository.existsByEmail("john@bank.com")).thenReturn(false);
        when(userRepository.existsByUsername("johnsmith")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(employeeProfileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toEmployeeResponse(any(), any())).thenReturn(UserResponse.builder().build());

        userService.createUser(employeeRequest);

        verify(customerProfileRepository, never()).existsByBsn(anyString());
    }
}
