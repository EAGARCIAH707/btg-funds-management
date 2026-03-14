package com.btg.funds.domain.model;

import com.btg.funds.domain.model.enums.TransactionType;

import java.math.BigDecimal;
import java.time.Instant;

public record Transaction(
        String transactionId,
        String clientId,
        String fundId,
        String fundName,
        TransactionType type,
        BigDecimal amount,
        Instant timestamp
) {
}
