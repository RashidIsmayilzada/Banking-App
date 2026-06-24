package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuditService auditService;

    // --Efe(Admin) — these mocks were removed; their methods now live in AccountService and TransactionService
//    @Mock private AccountRepository accountRepository;
//    @Mock private AccountMapper accountMapper;
//    @Mock private TransactionRepository transactionRepository;
//    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User employeeUser;
    private EmployeeProfile employeeProfile;

    @BeforeEach
    void setUp() {

        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setUsername("admin");
        adminUser.setRole(Role.EMPLOYEE);

        employeeUser = new User();
        employeeUser.setId(2L);
        employeeUser.setUsername("jdoe");
        employeeUser.setEmail("jdoe@bank.com");
        employeeUser.setActive(true);

        employeeProfile = new EmployeeProfile();
        employeeProfile.setUser(employeeUser);
        employeeProfile.setFirstName("John");
        employeeProfile.setLastName("Doe");
        employeeProfile.setEmployeeNumber("EMP123");
        employeeProfile.setEnabled(true);
    }

    @Test
    @DisplayName("Create Employee - Success")
    void createEmployee_Success() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setUsername("jdoe");
        request.setEmail("jdoe@bank.com");
        request.setPassword("Password123!");
        request.setEmployeeNumber("EMP123");
        request.setFirstName("John");
        request.setLastName("Doe");

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(employeeProfileRepository.existsByEmployeeNumber(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(employeeUser);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        EmployeeResponse response = adminService.createEmployee(request);

        assertNotNull(response);
        assertEquals("jdoe", response.getUsername());
        verify(employeeProfileRepository, times(1)).save(any(EmployeeProfile.class));
        verify(auditService, times(1)).record(any(), eq(AuditAction.EMPLOYEE_CREATED), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Create Employee - Fails on Duplicate Email")
    void createEmployee_DuplicateEmail() {
        EmployeeCreateRequest request = new EmployeeCreateRequest();
        request.setEmail("jdoe@bank.com");

        when(userRepository.existsByEmail("jdoe@bank.com")).thenReturn(true);

        assertThrows(DuplicateResourceException.class, () -> adminService.createEmployee(request));
    }

    @Test
    @DisplayName("Get All Employees")
    void getAllEmployees() {
        when(employeeProfileRepository.findAll()).thenReturn(List.of(employeeProfile));

        List<EmployeeResponse> responses = adminService.getAllEmployees();

        assertEquals(1, responses.size());
        assertEquals("John", responses.get(0).getFirstName());
    }

    @Test
    @DisplayName("Get Employee by ID - Success")
    void getEmployee_Success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(employeeProfileRepository.findByUser(employeeUser)).thenReturn(Optional.of(employeeProfile));

        EmployeeResponse response = adminService.getEmployee(2L);

        assertNotNull(response);
        assertEquals("jdoe@bank.com", response.getEmail());
    }

    @Test
    @DisplayName("Update Employee - Success")
    void updateEmployee_Success() {
        EmployeeUpdateRequest request = new EmployeeUpdateRequest();
        request.setFirstName("Jane");
        request.setLastName("Smith");

        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(employeeProfileRepository.findByUser(employeeUser)).thenReturn(Optional.of(employeeProfile));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        EmployeeResponse response = adminService.updateEmployee(2L, request);

        assertEquals("Jane", response.getFirstName());
        assertEquals("Smith", response.getLastName());
        verify(employeeProfileRepository, times(1)).save(employeeProfile);
    }

    @Test
    @DisplayName("Set Employee Status - Disable")
    void setEmployeeStatus() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(employeeProfileRepository.findByUser(employeeUser)).thenReturn(Optional.of(employeeProfile));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        EmployeeResponse response = adminService.setEmployeeStatus(2L, false);

        assertFalse(response.isActive());
        verify(userRepository, times(1)).save(employeeUser);
        verify(auditService, times(1)).record(any(), eq(AuditAction.EMPLOYEE_DISABLED), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Delete Employee - Success (Soft Delete)")
    void deleteEmployee() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(employeeUser));
        when(employeeProfileRepository.findByUser(employeeUser)).thenReturn(Optional.of(employeeProfile));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        adminService.deleteEmployee(2L);

        assertFalse(employeeUser.isActive());
        assertFalse(employeeProfile.isEnabled());
        verify(userRepository, times(1)).save(employeeUser);
        verify(employeeProfileRepository, times(1)).save(employeeProfile);
        verify(auditService, times(1)).record(any(), eq(AuditAction.EMPLOYEE_DELETED), anyString(), anyLong(), anyString());
    }

    // --Efe(Admin) — account service tests moved to AccountServiceTest
//    @Test
//    @DisplayName("Get All Accounts")
//    void getAllAccounts() { ... }
//
//    @Test
//    @DisplayName("Get Account by ID")
//    void getAccount() { ... }
//
//    @Test
//    @DisplayName("Freeze Account - Success")
//    void freezeAccount_Success() { ... }
//
//    @Test
//    @DisplayName("Freeze Account - Throws if already frozen")
//    void freezeAccount_AlreadyFrozen() { ... }
//
//    @Test
//    @DisplayName("Unfreeze Account - Success")
//    void unfreezeAccount_Success() { ... }
//
//    @Test
//    @DisplayName("Close Account - Success")
//    void closeAccount_Success() { ... }

    // --Efe(Admin) — transaction reversal tests moved to TransactionServiceTest
//    @Test
//    @DisplayName("Reverse Transaction - Transfer Success")
//    void reverseTransaction_Success() { ... }
//
//    @Test
//    @DisplayName("Reverse Transaction - Throws if already reversed")
//    void reverseTransaction_AlreadyReversed() { ... }

    // --Efe(Admin) — audit log tests moved to AuditService test coverage
//    @Test
//    @DisplayName("Get Audit Logs")
//    void getAuditLogs() { ... }
}
