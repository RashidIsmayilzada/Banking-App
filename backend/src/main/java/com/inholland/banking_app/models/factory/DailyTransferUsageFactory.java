package com.inholland.banking_app.models.factory;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public final class DailyTransferUsageFactory {

    private DailyTransferUsageFactory() {}

    public static DailyTransferUsage create(Account account, LocalDate date) {
        DailyTransferUsage usage = new DailyTransferUsage();
        usage.setAccount(account);
        usage.setUsageDate(date);
        usage.setTotalOutgoingAmount(BigDecimal.ZERO);
        usage.setUpdatedAt(LocalDateTime.now());
        return usage;
    }
}
