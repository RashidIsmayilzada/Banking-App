package com.inholland.banking_app.specifications;

import com.inholland.banking_app.dtos.TransactionFilterParams;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
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
                || params.getAccountId() != null
                || params.getIban() != null;

        if (!needsJoins) return;

        Join<Transaction, Account> from = root.join("fromAccount", JoinType.LEFT);
        Join<Transaction, Account> to = root.join("toAccount", JoinType.LEFT);

        // LEFT JOIN is used so that transactions with a null fromAccount or toAccount
        // (e.g. ATM deposits have no fromAccount) are not excluded from results.
        if (params.getUserId() != null) {
            predicates.add(cb.or(
                    cb.equal(from.get("customer").get("id"), params.getUserId()),
                    cb.equal(to.get("customer").get("id"), params.getUserId())
            ));
        }
        if (params.getAccountId() != null) {
            predicates.add(cb.or(
                    cb.equal(from.get("id"), params.getAccountId()),
                    cb.equal(to.get("id"), params.getAccountId())
            ));
        }
        if (params.getIban() != null) {
            predicates.add(cb.or(
                    cb.equal(from.get("iban"), params.getIban()),
                    cb.equal(to.get("iban"), params.getIban())
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
