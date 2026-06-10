package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {

    Page<Account> findByCustomerId(Long customerId, Pageable pageable);

    Page<Account> findByCustomerUsername(String username, Pageable pageable);

    Optional<Account> findByIban(String iban);

    boolean existsByIban(String iban);

    @Query("""
        SELECT a FROM Account a
        JOIN a.customer c
        JOIN c.customerProfile p
        WHERE a.accountType = com.inholland.banking_app.models.enums.AccountType.CHECKING
        AND a.status = com.inholland.banking_app.models.enums.AccountStatus.ACTIVE
        AND (LOWER(p.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
          OR LOWER(p.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
        """)
    List<Account> searchCheckingByCustomerName(@Param("name") String name);
}
