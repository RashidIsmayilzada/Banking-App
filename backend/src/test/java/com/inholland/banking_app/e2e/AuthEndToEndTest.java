package com.inholland.banking_app.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.inholland.banking_app.models.CustomerProfile;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthEndToEndTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

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

    private static final String TEST_EMAIL = "auth-e2e@test.com";
    private static final String TEST_USERNAME = "authe2e";
    private static final String TEST_PASSWORD = "Test1234!@#$";

    @BeforeEach
    void setUp() {
        cleanup();

        User user = new User();
        user.setEmail(TEST_EMAIL);
        user.setUsername(TEST_USERNAME);
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user = userRepository.save(user);

        CustomerProfile profile = new CustomerProfile();
        profile.setUser(user);
        profile.setFirstName("Auth");
        profile.setLastName("E2E");
        profile.setBsn("100200300");
        profile.setPhoneNumber("+31600102030");
        profile.setStatus(CustomerStatus.APPROVED);
        profile.setRegisteredAt(LocalDateTime.now());
        customerProfileRepository.save(profile);
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
    @DisplayName("Login with valid credentials returns a token and user info")
    void login_validCredentials_returnsTokenAndUserInfo() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.user.email").value(TEST_EMAIL))
                .andExpect(jsonPath("$.user.username").value(TEST_USERNAME))
                .andExpect(jsonPath("$.user.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void login_wrongPassword_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"WrongPassword!@#\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login with unknown email returns 401")
    void login_unknownEmail_returns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody@test.com\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login with invalid email format returns 422")
    void login_invalidEmailFormat_returns422() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("Logout blacklists the token — subsequent requests return 401")
    void logout_blacklistsToken_subsequentRequestsFail() throws Exception {
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully."));

        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token remains valid before logout")
    void token_isValidBeforeLogout() throws Exception {
        String loginResponse = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + TEST_EMAIL + "\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String token = objectMapper.readTree(loginResponse).get("accessToken").asText();

        mockMvc.perform(get("/transactions?page=0&size=10")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /users registers a new customer and returns 201")
    void register_newCustomer_returns201() throws Exception {
        String body = """
                {
                  "firstName": "New",
                  "lastName": "Customer",
                  "email": "new-reg@test.com",
                  "username": "newreg",
                  "password": "NewPassword1234!",
                  "bsn": "555666777",
                  "phoneNumber": "+31655566677"
                }
                """;

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("new-reg@test.com"))
                .andExpect(jsonPath("$.username").value("newreg"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    @DisplayName("Registering with a duplicate email returns 409")
    void register_duplicateEmail_returns409() throws Exception {
        String body = """
                {
                  "firstName": "Dup",
                  "lastName": "User",
                  "email": "%s",
                  "username": "dupuser",
                  "password": "Test1234!@#$",
                  "bsn": "777888999",
                  "phoneNumber": "+31677788899"
                }
                """.formatted(TEST_EMAIL);

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Login with a user that has no profile should still work or return a proper error")
    void login_noProfile_doesNotThrowUnexpectedError() throws Exception {
        // Create a user without a profile
        User user = new User();
        user.setEmail("no-profile@test.com");
        user.setUsername("noprofile");
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-profile@test.com\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.userStatus").isEmpty());
    }

    @Test
    @DisplayName("Login should fail with unauthorized for inactive users")
    void login_inactiveUser_returns403() throws Exception {
        User user = new User();
        user.setEmail("inactive@test.com");
        user.setUsername("inactive");
        user.setPasswordHash(passwordEncoder.encode(TEST_PASSWORD));
        user.setRole(Role.CUSTOMER);
        user.setActive(false);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"inactive@test.com\",\"password\":\"" + TEST_PASSWORD + "\"}"))
                .andExpect(status().isForbidden());
    }
}
