// --Efe(Admin)
package com.inholland.banking_app.controllers;

import com.inholland.banking_app.dtos.AuditLogResponse;
import com.inholland.banking_app.services.AuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Tag(name = "Admin Audit Logs", description = "Admin-only endpoints for accessing the system audit log")
public class AuditLogController {

    private final AuditService auditService;

    // --Efe(Admin)
    @Operation(summary = "Get Audit Logs",
            description = "Admin operation: retrieves the immutable system audit log, detailing all high-level actions taken by administrative users. Requires ADMIN role.")
    @GetMapping
    public ResponseEntity<List<AuditLogResponse>> getAuditLogs() {
        return ResponseEntity.ok(auditService.getAuditLogs());
    }
}
