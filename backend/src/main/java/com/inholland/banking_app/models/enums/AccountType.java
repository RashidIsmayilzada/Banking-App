package com.inholland.banking_app.models.enums;

public enum AccountType {
    CHECKING,
    SAVINGS;

    public boolean canInitiateTransfer() {
        return this == CHECKING;
    }
}
