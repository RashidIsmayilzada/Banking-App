package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.AtmSessionResponse;
import com.inholland.banking_app.dtos.MoneyResponse;
import com.inholland.banking_app.dtos.TransactionPartyResponse;
import com.inholland.banking_app.dtos.TransactionResponse;
import com.inholland.banking_app.dtos.TransferResultResponse;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.AtmSession;
import com.inholland.banking_app.models.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class AtmMapper {

    private static final String CURRENCY = "EUR";

    public AtmSessionResponse toAtmSessionResponse(AtmSession session, String token) {
        AtmSessionResponse response = new AtmSessionResponse();
        response.setSessionId(session.getId());
        response.setSessionToken(token);
        response.setCustomerUserId(session.getCustomer().getId());
        response.setStartedAt(session.getStartedAt());
        response.setSuccessfulLogin(session.isSuccessfulLogin());
        return response;
    }

    public TransferResultResponse toTransferResultResponse(Transaction transaction, Account account) {
        TransferResultResponse result = new TransferResultResponse();
        result.setTransaction(toTransactionResponse(transaction));
        result.setSourceBalance(toMoneyResponse(account.getBalance()));
        return result;
    }

    public TransactionResponse toTransactionResponse(Transaction transaction) {
        TransactionResponse dto = new TransactionResponse();
        dto.setTransactionId(transaction.getId());
        dto.setTransactionType(transaction.getTransactionType());
        dto.setAmount(toMoneyResponse(transaction.getAmount()));
        dto.setFromAccount(toTransactionPartyResponse(transaction.getFromAccount()));
        dto.setToAccount(toTransactionPartyResponse(transaction.getToAccount()));
        dto.setChannel(transaction.getChannel());
        dto.setInitiatedByUserId(transaction.getInitiatedBy().getId());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setDescription(transaction.getDescription());
        return dto;
    }

    public TransactionPartyResponse toTransactionPartyResponse(Account account) {
        if (account == null) {
            return null;
        }
        TransactionPartyResponse party = new TransactionPartyResponse();
        party.setAccountId(account.getId());
        party.setIban(account.getIban());
        party.setName(account.getCustomer().getCustomerProfile().getFirstName()
                + " " + account.getCustomer().getCustomerProfile().getLastName());
        party.setUserId(account.getCustomer().getId());
        return party;
    }

    public MoneyResponse toMoneyResponse(BigDecimal amount) {
        MoneyResponse money = new MoneyResponse();
        money.setAmount(amount);
        money.setCurrency(CURRENCY);
        return money;
    }
}
