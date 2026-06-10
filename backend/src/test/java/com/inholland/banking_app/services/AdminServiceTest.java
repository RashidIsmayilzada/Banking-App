package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.*;
import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.exceptions.DuplicateResourceException;
import com.inholland.banking_app.mappers.AccountMapper;
import com.inholland.banking_app.models.*;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.models.enums.TransactionType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmployeeProfileRepository employeeProfileRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountMapper accountMapper;
    @Mock private TransactionRepository transactionRepository;
    @Mock private AuditService auditService;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    private User adminUser;
    private User employeeUser;
    private EmployeeProfile employeeProfile;
    private Account account;

    @BeforeEach
    void setUp() {

        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin");
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
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

        account = new Account();
        account.setId(10L);
        account.setBalance(new BigDecimal("1000.00"));
        account.setStatus(AccountStatus.ACTIVE);
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


    @Test
    @DisplayName("Get All Accounts")
    void getAllAccounts() {
        when(accountRepository.findAll()).thenReturn(List.of(account));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().build());

        List<AccountResponse> responses = adminService.getAllAccounts();

        assertEquals(1, responses.size());
    }

    @Test
    @DisplayName("Get Account by ID")
    void getAccount() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().build());

        assertNotNull(adminService.getAccount(10L));
    }

    @Test
    @DisplayName("Freeze Account - Success")
    void freezeAccount_Success() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().build());

        adminService.freezeAccount(10L);

        assertTrue(account.isFrozen());
        verify(accountRepository, times(1)).save(account);
        verify(auditService, times(1)).record(any(), eq(AuditAction.ACCOUNT_FROZEN), anyString(), anyLong(), anyString());
    }

    @Test

    @DisplayName("Freeze Account - Throws if already frozen")
    void freezeAccount_AlreadyFrozen() {
        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));

        assertThrows(AccountStateException.class, () -> adminService.freezeAccount(10L));
    }

    @Test
    @DisplayName("Unfreeze Account - Success")
    void unfreezeAccount_Success() {

        account.setStatus(AccountStatus.FROZEN);
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().build());

        adminService.unfreezeAccount(10L);

        assertFalse(account.isFrozen());
        verify(accountRepository, times(1)).save(account);
    }

    @Test
    @DisplayName("Close Account - Success")
    void closeAccount_Success() {
        when(accountRepository.findById(10L)).thenReturn(Optional.of(account));
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(accountMapper.toResponse(account)).thenReturn(AccountResponse.builder().build());

        adminService.closeAccount(10L);

        assertTrue(account.isClosed());
        verify(accountRepository, times(1)).save(account);
        verify(auditService, times(1)).record(any(), eq(AuditAction.ACCOUNT_CLOSED), anyString(), anyLong(), anyString());
    }



    @Test
    @DisplayName("Reverse Transaction - Transfer Success")
    void reverseTransaction_Success() {
        Account toAccount = new Account();
        toAccount.setId(20L);
        toAccount.setBalance(new BigDecimal("500.00"));
        toAccount.setStatus(AccountStatus.ACTIVE);

        Transaction originalTx = new Transaction();
        originalTx.setId(100L);
        originalTx.setTransactionType(TransactionType.TRANSFER);
        originalTx.setAmount(new BigDecimal("100.00"));
        originalTx.setFromAccount(account); // balance 1000
        originalTx.setToAccount(toAccount); // balance 500

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(originalTx));
        when(transactionRepository.existsByReversesTransactionId(100L)).thenReturn(false);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction t = invocation.getArgument(0);
            t.setId(999L);
            return t;
        });

        TransactionReversalResponse response = adminService.reverseTransaction(100L, "admin");


        assertEquals(new BigDecimal("400.00"), toAccount.getBalance());
        assertEquals(new BigDecimal("1100.00"), account.getBalance());

        assertNotNull(response);
        assertEquals(100L, response.getOriginalTransactionId());
        assertEquals(TransactionType.REVERSAL.name(), response.getTransactionType());
        assertNotNull(response.getReversalTransactionId()); // now 999L, not null
        verify(transactionRepository, times(1)).save(any(Transaction.class));
        verify(auditService, times(1)).record(any(), eq(AuditAction.TRANSACTION_REVERSED), anyString(), anyLong(), anyString());
    }

    @Test
    @DisplayName("Reverse Transaction - Throws if already reversed")
    void reverseTransaction_AlreadyReversed() {
        Transaction originalTx = new Transaction();
        originalTx.setId(100L);
        originalTx.setTransactionType(TransactionType.TRANSFER);

        when(transactionRepository.findById(100L)).thenReturn(Optional.of(originalTx));
        when(transactionRepository.existsByReversesTransactionId(100L)).thenReturn(true);

        assertThrows(IllegalStateException.class, () -> adminService.reverseTransaction(100L, "admin"));
    }


    @Test
    @DisplayName("Get Audit Logs")
    void getAuditLogs() {
        AuditLog log = new AuditLog();
        log.setId(1L);
        log.setActorUsername("admin");
        log.setAction(AuditAction.EMPLOYEE_CREATED);

        when(auditLogRepository.findAll()).thenReturn(List.of(log));

        List<AuditLogResponse> logs = adminService.getAuditLogs();

        assertEquals(1, logs.size());
        assertEquals("admin", logs.get(0).getActorUsername());
        assertEquals(AuditAction.EMPLOYEE_CREATED.name(), logs.get(0).getAction());
    }
}