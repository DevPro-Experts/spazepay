package com.spazepay.dto.transaction;

import com.spazepay.model.enums.TransactionType;
import lombok.Data;

import java.time.Instant;

@Data
public class TransactionResponse {
    private Long id;
    private TransactionType type;
    private String amount;
    private Instant transactionDate;
    private String description;
}