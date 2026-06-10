package com.inholland.banking_app.specifications;

import com.inholland.banking_app.dtos.UserFilterRequest;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.User;
import com.inholland.banking_app.models.enums.CustomerStatus;
import com.inholland.banking_app.models.enums.Role;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    private UserSpecification() {}

    public static Specification<User> fromFilter(UserFilterRequest filter) {
        return Specification.allOf(
                hasRole(filter.getRole()),
                hasActive(filter.getActive()),
                hasAccount(filter.getHasAccount()),
                hasCustomerStatus(filter.getStatus()),
                containsSearch(filter.getSearch()));
    }

    private static Specification<User> hasRole(String role) {
        return (root, query, cb) -> {
            if (role == null || role.isBlank()) return null;
            return cb.equal(root.get("role"), Role.valueOf(role.trim().toUpperCase()));
        };
    }

    private static Specification<User> hasActive(Boolean active) {
        return (root, query, cb) -> active == null ? null : cb.equal(root.get("active"), active);
    }

    private static Specification<User> hasAccount(Boolean hasAccount) {
        return (root, query, cb) -> {
            if (hasAccount == null) return null;
            return hasAccount
                    ? cb.isNotEmpty(root.get("accounts"))
                    : cb.isEmpty(root.get("accounts"));
        };
    }

    private static Specification<User> hasCustomerStatus(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) return null;
            return cb.equal(root.get("customerProfile").get("status"),
                    CustomerStatus.valueOf(status.trim().toUpperCase()));
        };
    }

    private static Specification<User> containsSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isBlank()) return null;

            String pattern = "%" + search.trim().toLowerCase() + "%";
            String ibanPattern = "%" + search.trim().replaceAll("\\s+", "").toLowerCase() + "%";

            Subquery<Long> ibanSubquery = query.subquery(Long.class);
            Root<Account> accountRoot = ibanSubquery.from(Account.class);
            ibanSubquery.select(accountRoot.get("customer").get("id"))
                    .where(cb.like(cb.lower(accountRoot.get("iban")), ibanPattern));

            return cb.or(
                    root.get("id").in(ibanSubquery),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(root.get("username")), pattern),
                    cb.like(cb.lower(root.get("customerProfile").get("firstName")), pattern),
                    cb.like(cb.lower(root.get("customerProfile").get("lastName")), pattern)
            );
        };
    }
}
