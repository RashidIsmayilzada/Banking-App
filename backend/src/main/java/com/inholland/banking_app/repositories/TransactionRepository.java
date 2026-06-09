package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, JpaSpecificationExecutor<Transaction> {

    @Query("""
        SELECT coalesce(sum(t.amount), 0)
        FROM Transaction t\s
        WHERE t.fromAccount.id = :accountId
            AND t.transactionType = com.inholland.banking_app.models.enums.TransactionType.TRANSFER
            AND t.createdAt >= :startOfDay
            AND t.createdAt < :endOfDay
       \s
   \s""")
    BigDecimal sumOutgoingAmountByAccountIdAndDate(
            @Param("accountId") Long accountId,
            @Param("startOfDay") LocalDate startOfDay,
            @Param("endOfDay") LocalDate endOfDay

    );

    boolean existsByReversesTransactionId(Long id);

}
