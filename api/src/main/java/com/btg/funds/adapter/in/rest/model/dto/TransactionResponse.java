package com.btg.funds.adapter.in.rest.model.dto;

import com.btg.funds.domain.model.Transaction;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(
        String transactionId,
        String fundId,
        String fundName,
        String type,
        BigDecimal amount,
        Instant timestamp
) {

    public static TransactionResponse from(Transaction tx) {
        return new TransactionResponse(
                tx.transactionId(),
                tx.fundId(),
                tx.fundName(),
                tx.type().name(),
                tx.amount(),
                tx.timestamp()
        );
    }
}
