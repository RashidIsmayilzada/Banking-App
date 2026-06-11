package com.inholland.banking_app.dtos;

import com.inholland.banking_app.models.enums.Channel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionFilterParams {
    private int page;
    private int size;
    private String sort;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateTime;
    private Double amountMin;
    private Double amountMax;
    private String iban;
    private Long userId;
    private Channel channel;
    private Double amountEquals;
}
