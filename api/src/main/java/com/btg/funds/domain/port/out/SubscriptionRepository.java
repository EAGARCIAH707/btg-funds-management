package com.btg.funds.domain.port.out;

import com.btg.funds.domain.model.Subscription;

import java.util.Optional;

public interface SubscriptionRepository {

    Optional<Subscription> findByClientAndFund(String clientId, String fundId);

    void save(Subscription subscription);

    void delete(String clientId, String fundId);
}
