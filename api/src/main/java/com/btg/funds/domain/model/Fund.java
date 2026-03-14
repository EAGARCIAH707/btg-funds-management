package com.btg.funds.domain.model;

import java.math.BigDecimal;

public record Fund(
        String id,
        String name,
        BigDecimal minimumAmount,
        String category
) {
}
