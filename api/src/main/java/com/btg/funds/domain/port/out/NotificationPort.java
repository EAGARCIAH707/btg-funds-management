package com.btg.funds.domain.port.out;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.enums.TransactionType;

public interface NotificationPort {

    void notify(Client client, Fund fund, TransactionType type);
}
