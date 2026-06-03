package com.inholland.banking_app.exceptions;

/**
 * Thrown when an operation is not allowed given the account's current state,
 * e.g. trying to modify or close an account that is already closed.
 * <p>
 * This is a client-caused, expected condition (HTTP 409 Conflict), not a
 * server fault.
 */
public class AccountStateException extends RuntimeException {
    public AccountStateException(String message) {
        super(message);
    }
}
