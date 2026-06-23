// --Efe(Admin)
package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AuditLogResponse;
import com.inholland.banking_app.security.JwtAuthenticationFilter;
import com.inholland.banking_app.services.AuditService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.SecurityAutoConfiguration;
import org.springframework.boot.security.autoconfigure.web.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = AuditLogController.class,
        excludeAutoConfiguration = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class},
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = JwtAuthenticationFilter.class)
)
class AuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditService auditService;

    // --Efe(Admin)
    @Test
    @DisplayName("GET /audit-logs - should return 200 with a list of audit log entries")
    void getAuditLogs_shouldReturn200_withAuditLogList() throws Exception {
        AuditLogResponse entry = new AuditLogResponse();
        entry.setId(1L);
        entry.setActorUsername("admin");
        entry.setAction("ACCOUNT_FROZEN");
        entry.setTargetType("ACCOUNT");
        entry.setDetails("Froze account: NL01INHO0000000010");
        entry.setCreatedAt(LocalDateTime.of(2026, 6, 22, 10, 0));

        when(auditService.getAuditLogs()).thenReturn(List.of(entry));

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].actorUsername").value("admin"))
                .andExpect(jsonPath("$[0].action").value("ACCOUNT_FROZEN"))
                .andExpect(jsonPath("$[0].targetType").value("ACCOUNT"));
    }

    // --Efe(Admin)
    @Test
    @DisplayName("GET /audit-logs - should return 200 with empty list when no logs exist")
    void getAuditLogs_shouldReturn200_withEmptyList_whenNoLogsExist() throws Exception {
        when(auditService.getAuditLogs()).thenReturn(List.of());

        mockMvc.perform(get("/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }
}
