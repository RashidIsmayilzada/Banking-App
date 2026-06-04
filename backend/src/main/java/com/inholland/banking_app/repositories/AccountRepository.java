package com.inholland.banking_app.repositories;

import com.inholland.banking_app.models.Account;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByIban(String iban);

    Page<Account> findByCustomerId(Pageable pageable, Long customerId);

    boolean existsByIban(String iban);
}
