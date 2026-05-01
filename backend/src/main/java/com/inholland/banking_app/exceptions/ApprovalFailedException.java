package com.inholland.banking_app.exceptions;

public class ApprovalFailedException extends RuntimeException {
    public ApprovalFailedException(String message) {
        super(message);
    }
}