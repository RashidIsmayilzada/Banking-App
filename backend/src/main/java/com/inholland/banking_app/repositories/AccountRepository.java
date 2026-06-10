package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    Page<Account> findByCustomerUsername(String username, Pageable pageable);

    Optional<Account> findByIban(String iban);

    boolean existsByIban(String iban);

    // matching account, not just the ones on the current page.
    @Query("SELECT coalesce(sum(a.balance), 0) FROM Account a")
    BigDecimal sumBalance();

    @Query("SELECT coalesce(sum(a.balance), 0) FROM Account a WHERE a.customer.id = :customerId")
    BigDecimal sumBalanceByCustomerId(@Param("customerId") Long customerId);

    @Query("SELECT coalesce(sum(a.balance), 0) FROM Account a WHERE a.customer.username = :username")
    BigDecimal sumBalanceByCustomerUsername(@Param("username") String username);
}
