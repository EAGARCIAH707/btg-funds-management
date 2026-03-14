package com.btg.funds.domain.port.in;

import com.btg.funds.domain.model.Transaction;

public interface CancelSubscriptionUseCase {

    record Command(String clientId, String fundId) {}

    Transaction execute(Command command);
}
