package com.inholland.banking_app.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AtmSessionResponse {
    private Long sessionId;
    private String sessionToken;
    private Long customerUserId;
    private LocalDateTime startedAt;
    private boolean successfulLogin;
}
