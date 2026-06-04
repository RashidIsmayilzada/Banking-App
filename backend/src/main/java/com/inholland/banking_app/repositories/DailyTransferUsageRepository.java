package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.DailyTransferUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface DailyTransferUsageRepository extends JpaRepository<DailyTransferUsage, Long> {
    Optional<DailyTransferUsage> findByAccountAndUsageDate(Account account, LocalDate usageDate);
}
