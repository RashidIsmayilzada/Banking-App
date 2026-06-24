// --Efe(Admin)
package com.inholland.banking_app.controllers;

import com.inholland.banking_app.models.AuditLog;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.LocalDateTime;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Functional tests for GET /audit-logs.
 * Full application context, real security filter chain.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuditLogFunctionalTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // --- Security ---

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLogs_asAdmin_returnsOk() throws Exception {
        seedAuditLog("admin", AuditAction.ACCOUNT_FROZEN, "ACCOUNT", 1L, "Froze account NL01INHO0000000001");

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$[0].action").value("ACCOUNT_FROZEN"))
                .andExpect(jsonPath("$[0].targetType").value("ACCOUNT"));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLogs_asAdmin_returnsEmptyList_whenNoLogs() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void getAuditLogs_asAdmin_returnsMultipleLogs() throws Exception {
        seedAuditLog("admin", AuditAction.ACCOUNT_FROZEN,    "ACCOUNT",     1L, "Froze account");
        seedAuditLog("admin", AuditAction.ACCOUNT_UNFROZEN,  "ACCOUNT",     1L, "Unfroze account");
        seedAuditLog("admin", AuditAction.TRANSACTION_REVERSED, "TRANSACTION", 5L, "Reversed transaction #5");

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(3));
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "employee", roles = "EMPLOYEE")
    void getAuditLogs_asEmployee_isForbidden() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    @WithMockUser(username = "customer", roles = "CUSTOMER")
    void getAuditLogs_asCustomer_isForbidden() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isForbidden());
    }

    // --Efe(Admin)
    @Test
    void getAuditLogs_anonymous_isUnauthorized() throws Exception {
        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    // --- Fixture ---

    private void seedAuditLog(String actorUsername, AuditAction action, String targetType, Long targetId, String details) {
        AuditLog log = new AuditLog();
        log.setActorUsername(actorUsername);
        log.setAction(action);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDetails(details);
        log.setCreatedAt(LocalDateTime.now());
        auditLogRepository.save(log);
    }
}
