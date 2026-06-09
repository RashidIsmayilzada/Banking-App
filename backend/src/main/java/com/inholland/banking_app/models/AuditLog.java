package com.inholland.banking_app.models;

import com.inholland.banking_app.models.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@NoArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) //auto increment
    private Long id;

    private Long actorId;

    private String actorUsername;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuditAction action;

    private String targetType;

    private Long targetId;

    private String details;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}