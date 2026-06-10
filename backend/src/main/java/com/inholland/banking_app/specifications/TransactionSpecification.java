package com.inholland.banking_app.specifications;

import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.models.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// A Specification builds a dynamic SQL WHERE clause at runtime.
// Instead of writing a separate repository method for every filter combination,
// we collect only the filters the caller actually provided and combine them with AND.
// Spring Data JPA passes this into findAll() and generates the query automatically.
public class TransactionSpecification {

    private TransactionSpecification() {}

    // Entry point — delegates each filter group to its own helper,
    // then merges all collected predicates into a single AND condition.
    public static Specification<Transaction> fromParams(TransactionFilterParams params) {
        return (root, query, cb) -> {
            // Ensure we don't get duplicate transactions due to LEFT JOINs on accounts
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.distinct(true);
            }

            List<Predicate> predicates = new ArrayList<>();

            addPartyPredicates(predicates, root, params, cb);
            addChannelPredicate(predicates, root, params, cb);
            addDatePredicates(predicates, root, params, cb);
            addAmountPredicates(predicates, root, params, cb);

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // Filters by the user, account, or IBAN involved in the transaction.
    // A transaction counts as a match if either the fromAccount or toAccount satisfies the condition,
    // so each predicate is an OR across both sides.
    // Joining fromAccount and toAccount is only done when at least one of these filters is active,
    // because SQL JOINs have a cost and are pointless when no account filter was requested.
    private static void addPartyPredicates(List<Predicate> predicates, Root<Transaction> root,
                                           TransactionFilterParams params, CriteriaBuilder cb) {
        boolean needsJoins = params.getUserId() != null
                || params.getIban() != null;

        if (!needsJoins) return;

        // We use LEFT JOIN for accounts so that transactions with only one side 
        // (like ATM deposits/withdrawals) are not dropped from the results.
        Join<Transaction, Account> fromAccount = root.join("fromAccount", JoinType.LEFT);
        Join<Transaction, Account> toAccount = root.join("toAccount", JoinType.LEFT);

        if (params.getUserId() != null) {
            // We also need to LEFT JOIN from Account to User. 
            // If we just used fromAccount.get("customer"), JPA would often use an implicit INNER JOIN,
            // which would exclude any transaction where fromAccount is null.
            Join<Account, User> fromCustomer = fromAccount.join("customer", JoinType.LEFT);
            Join<Account, User> toCustomer = toAccount.join("customer", JoinType.LEFT);

            predicates.add(cb.or(
                    cb.equal(fromCustomer.get("id"), params.getUserId()),
                    cb.equal(toCustomer.get("id"), params.getUserId())
            ));
        }
        if (params.getIban() != null) {
            predicates.add(cb.or(
                    cb.equal(fromAccount.get("iban"), params.getIban()),
                    cb.equal(toAccount.get("iban"), params.getIban())
            ));
        }
    }

    // Filters by the channel the transaction was made through (WEB, ATM, EMPLOYEE).
    // Channel sits directly on the Transaction row so no join is needed.
    private static void addChannelPredicate(List<Predicate> predicates, Root<Transaction> root,
                                            TransactionFilterParams params, CriteriaBuilder cb) {
        if (params.getChannel() != null) {
            predicates.add(cb.equal(root.get("channel"), params.getChannel()));
        }
    }

    // Filters by a date range on createdAt.
    // startDateTime is inclusive (>=), endDateTime is inclusive (<=).
    // Either bound can be omitted to leave that side of the range open.
    private static void addDatePredicates(List<Predicate> predicates, Root<Transaction> root,
                                          TransactionFilterParams params, CriteriaBuilder cb) {
        if (params.getStartDateTime() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), params.getStartDateTime()));
        }
        if (params.getEndDateTime() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), params.getEndDateTime()));
        }
    }

    // Filters by transaction amount.
    // amountMin and amountMax define an inclusive range.
    // amountEquals is an exact match and takes priority if all three are somehow provided.
    private static void addAmountPredicates(List<Predicate> predicates, Root<Transaction> root,
                                            TransactionFilterParams params, CriteriaBuilder cb) {
        if (params.getAmountMin() != null) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(params.getAmountMin())));
        }
        if (params.getAmountMax() != null) {
            predicates.add(cb.lessThanOrEqualTo(root.get("amount"), BigDecimal.valueOf(params.getAmountMax())));
        }
        if (params.getAmountEquals() != null) {
            predicates.add(cb.equal(root.get("amount"), BigDecimal.valueOf(params.getAmountEquals())));
        }
    }
}
