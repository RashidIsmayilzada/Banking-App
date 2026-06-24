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

    // Build user specification with the dynamic filter request
    // Set allof so it return all filter together and silently ignore null fields 
    public static Specification<User> fromFilter(UserFilterRequest filter) {
        return Specification.allOf(
                hasRole(filter.getRole()),
                hasActive(filter.getActive()),
                hasAccount(filter.getHasAccount()),
                hasCustomerStatus(filter.getStatus()),
                containsSearch(filter.getSearch()));
    }

    // Check if user has role
    private static Specification<User> hasRole(String role) {
        // Root = the db role column 
        // query = query object
        // cb = CriteriaBuilder, tool box for building conditions 
        return (root, query, cb) -> {
            if (role == null || role.isBlank()) return null;
            return cb.equal(root.get("role"), Role.valueOf(role.trim().toUpperCase()));
        };
    }

    // hasActive return true or false and null
    private static Specification<User> hasActive(Boolean active) {
        return (root, query, cb)
                -> active == null ? null : cb.equal(root.get("active"), active);
    }

    // hasAccount return list of users who has account and also who don't have account yet
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

            // Build a subquery that looks into customer table
            // Get the Id of customer that match the Iban 
            Subquery<Long> ibanSubquery = query.subquery(Long.class);
            Root<Account> accountRoot = ibanSubquery.from(Account.class);
            ibanSubquery.select(accountRoot.get("customer").get("id"))
                    .where(cb.like(cb.lower(accountRoot.get("iban")), ibanPattern));

            // One search term checks all the db column to return its match
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
