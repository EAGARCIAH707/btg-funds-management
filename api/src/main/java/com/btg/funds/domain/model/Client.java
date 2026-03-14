package com.btg.funds.domain.model;

import com.btg.funds.domain.model.enums.NotificationPreference;

import java.math.BigDecimal;

public record Client(
        String id,
        String name,
        String email,
        String phone,
        BigDecimal balance,
        NotificationPreference notificationPreference
) {

    public boolean hasEnoughBalance(BigDecimal amount) {
        return balance.compareTo(amount) >= 0;
    }

    public Client debit(BigDecimal amount) {
        return new Client(id, name, email, phone, balance.subtract(amount), notificationPreference);
    }

    public Client credit(BigDecimal amount) {
        return new Client(id, name, email, phone, balance.add(amount), notificationPreference);
    }
}
