package com.inholland.banking_app.mappers;

import com.inholland.banking_app.dtos.MoneyDto;
import com.inholland.banking_app.dtos.PageMetadataDto;
import com.inholland.banking_app.dtos.TransactionDto;
import com.inholland.banking_app.dtos.TransactionPageDto;
import com.inholland.banking_app.dtos.TransactionPartyDto;
import com.inholland.banking_app.models.Account;
import com.inholland.banking_app.models.Transaction;
import com.inholland.banking_app.repositories.CustomerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TransactionMapper {

    private final CustomerProfileRepository customerProfileRepository;

    public TransactionDto toDto(Transaction transaction) {
        return TransactionDto.builder()
                .transactionId(transaction.getId())
                .transactionType(transaction.getTransactionType())
                .amount(MoneyDto.builder()
                        .amount(transaction.getAmount().doubleValue())
                        .currency(transaction.getCurrency())
                        .build())
                .fromAccount(toPartyDto(transaction.getFromAccount()))
                .toAccount(toPartyDto(transaction.getToAccount()))
                .channel(transaction.getChannel())
                .initiatedByUserId(transaction.getInitiatedBy().getId())
                .createdAt(transaction.getCreatedAt())
                .description(transaction.getDescription())
                .build();
    }

    public TransactionPageDto toPageDto(Page<Transaction> page) {
        return TransactionPageDto.builder()
                .items(page.getContent().stream().map(this::toDto).toList())
                .page(PageMetadataDto.builder()
                        .page(page.getNumber())
                        .size(page.getSize())
                        .totalElements(page.getTotalElements())
                        .totalPages(page.getTotalPages())
                        .build())
                .build();
    }

    private TransactionPartyDto toPartyDto(Account account) {
        if (account == null) return null;
        String name = customerProfileRepository.findById(account.getCustomer().getId())
                .map(p -> p.getFirstName() + " " + p.getLastName())
                .orElse(account.getCustomer().getUsername());
        return TransactionPartyDto.builder()
                .accountId(account.getId())
                .iban(account.getIban())
                .name(name)
                .userId(account.getCustomer().getId())
                .build();
    }
}
