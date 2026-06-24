package com.inholland.banking_app.services;

import com.inholland.banking_app.models.AuditLog;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AuditAction;
import com.inholland.banking_app.repositories.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

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
}