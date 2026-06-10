package com.inholland.banking_app.security;

import com.inholland.banking_app.models.CustomerProfile;
import com.inholland.banking_app.models.EmployeeProfile;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import com.inholland.banking_app.repositories.AccountRepository;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import com.inholland.banking_app.repositories.DailyTransferUsageRepository;
import com.inholland.banking_app.repositories.EmployeeProfileRepository;
import com.inholland.banking_app.repositories.TransactionRepository;
import com.inholland.banking_app.repositories.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private TokenBlacklistService tokenBlacklistService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerProfileRepository customerProfileRepository;

    @Autowired
    private EmployeeProfileRepository employeeProfileRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private DailyTransferUsageRepository dailyTransferUsageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String customerToken;
    private String employeeToken;

    @BeforeEach
    void setUp() {
        cleanup();

        User customer = new User();
        customer.setEmail("sec-customer@test.com");
        customer.setUsername("sec-customer");
        customer.setPasswordHash(passwordEncoder.encode("Test1234!@#$"));
        customer.setRole(Role.CUSTOMER);
        customer.setActive(true);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customer = userRepository.save(customer);

        CustomerProfile customerProfile = new CustomerProfile();
        customerProfile.setUser(customer);
        customerProfile.setFirstName("Sec");
        customerProfile.setLastName("Customer");
        customerProfile.setBsn("111222333");
        customerProfile.setPhoneNumber("+31600000001");
        customerProfile.setStatus(CustomerStatus.APPROVED);
        customerProfile.setRegisteredAt(LocalDateTime.now());
        customerProfileRepository.save(customerProfile);

        User employee = new User();
        employee.setEmail("sec-employee@test.com");
        employee.setUsername("sec-employee");
        employee.setPasswordHash(passwordEncoder.encode("Test1234!@#$"));
        employee.setRole(Role.EMPLOYEE);
        employee.setActive(true);
        employee.setCreatedAt(LocalDateTime.now());
        employee.setUpdatedAt(LocalDateTime.now());
        employee = userRepository.save(employee);

        EmployeeProfile employeeProfile = new EmployeeProfile();
        employeeProfile.setUser(employee);
        employeeProfile.setFirstName("Sec");
        employeeProfile.setLastName("Employee");
        employeeProfile.setEmployeeNumber("EMP-SEC-001");
        employeeProfile.setEnabled(true);
        employeeProfile.setCreatedAt(LocalDateTime.now());
        employeeProfileRepository.save(employeeProfile);

        customerToken = jwtUtil.generateToken(customer.getUsername());
        employeeToken = jwtUtil.generateToken(employee.getUsername());
    }

    @AfterEach
    void cleanup() {
        dailyTransferUsageRepository.deleteAll();
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        customerProfileRepository.deleteAll();
        employeeProfileRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /transactions without token returns 401")
    void getTransactions_noToken_returns401() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /transactions with malformed token returns 401")
    void getTransactions_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /transactions with blacklisted token returns 401")
    void getTransactions_blacklistedToken_returns401() throws Exception {
        tokenBlacklistService.blacklist(customerToken, jwtUtil.getExpirationFromToken(customerToken));

        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /transactions with valid customer token returns 200")
    void getTransactions_validCustomerToken_returns200() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /transactions with valid employee token returns 200")
    void getTransactions_validEmployeeToken_returns200() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /transactions without token returns 401")
    void postTransactions_noToken_returns401() throws Exception {
        mockMvc.perform(post("/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"DEPOSIT\",\"iban\":\"NL01INHO0000000001\",\"amount\":100.0,\"description\":\"test\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login is publicly accessible without a token")
    void postAuthLogin_isPublic() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"sec-customer@test.com\",\"password\":\"Test1234!@#$\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /users is publicly accessible without a token")
    void postUsers_isPublic() throws Exception {
        String body = """
                {
                  "firstName": "Public",
                  "lastName": "Test",
                  "email": "public-reg@test.com",
                  "username": "publicreg",
                  "password": "Test1234!@#$",
                  "bsn": "999000111",
                  "phoneNumber": "+31699900011"
                }
                """;
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Authorization header without Bearer prefix is rejected")
    void getTransactions_noBearerPrefix_returns401() throws Exception {
        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", customerToken))
                .andExpect(status().isUnauthorized());
    }
}
