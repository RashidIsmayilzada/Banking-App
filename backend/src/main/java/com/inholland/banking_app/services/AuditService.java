package com.inholland.banking_app.services;

import com.inholland.banking_app.dtos.AuditLogResponse;
import com.inholland.banking_app.models.AuditLog;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void record(User actor, AuditAction action, String targetType, Long targetId, String details) {
        AuditLog auditLog = new AuditLog();

        if (actor != null) {
            auditLog.setActorId(actor.getId());
            auditLog.setActorUsername(actor.getUsername());
        }

        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetails(details);
        auditLog.setCreatedAt(LocalDateTime.now());

        auditLogRepository.save(auditLog);
    }

    // --Efe(Admin)
    public List<AuditLogResponse> getAuditLogs() {
        return auditLogRepository.findAll().stream()
                .map(this::toAuditLogResponse)
                .collect(Collectors.toList());
    }

    // --Efe(Admin)
    private AuditLogResponse toAuditLogResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setId(log.getId());
        response.setActorId(log.getActorId());
        response.setActorUsername(log.getActorUsername());

        if (log.getAction() != null) {
            response.setAction(log.getAction().name());
        }

        response.setTargetType(log.getTargetType());
        response.setTargetId(log.getTargetId());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }
}
