package com.inholland.banking_app.policies;

import com.inholland.banking_app.exceptions.AccountStateException;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.AccountStatus;
import com.inholland.banking_app.models.enums.Role;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Holds the business rules and authorization decisions for accounts
 * The {@link Account} entity only carries state and performs raw mutations
 */
@Component
public class AccountPolicy {

    // Used to scope the account list: employees see all, customers see their own.
    public boolean isEmployee(Authentication authentication) {
        String employeeAuthority = "ROLE_" + Role.EMPLOYEE.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> employeeAuthority.equals(authority.getAuthority()));
    }

    // A closed account is frozen: its transfer limits can no longer be changed.
    public void assertCanUpdateLimits(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException("Cannot update a closed account");
        }
    }

    //An account can only be closed once.
    public void assertCanClose(Account account) {
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new AccountStateException("Account is already closed");
        }
    }

    //Ownership rule: the account belongs to the given user.
    public boolean isOwnedBy(Account account, User user) {
        return account.getCustomer().getId().equals(user.getId());
    }

    // Access rule: employees may access any account, customers only their own.
    public boolean canAccess(Account account, User user) {
        return user.getRole() == Role.EMPLOYEE || isOwnedBy(account, user);
    }
}