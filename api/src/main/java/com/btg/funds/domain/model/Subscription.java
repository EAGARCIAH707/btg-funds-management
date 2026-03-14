package com.btg.funds.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Subscription(
        String clientId,
        String fundId,
        String fundName,
        BigDecimal amount,
        Instant subscribedAt
) {
}
