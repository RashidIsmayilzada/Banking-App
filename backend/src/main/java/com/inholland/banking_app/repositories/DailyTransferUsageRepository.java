package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.DailyTransferUsage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;

public interface DailyTransferUsageRepository extends JpaRepository<DailyTransferUsage, Long> {

    Optional<DailyTransferUsage> findByAccountIdAndUsageDate(Long accountId, LocalDate usageDate);
}
