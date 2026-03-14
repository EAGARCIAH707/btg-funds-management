package com.btg.funds.domain.port.out;

import com.btg.funds.domain.model.PageResult;
import com.btg.funds.domain.model.Transaction;

public interface TransactionRepository {

    void save(Transaction transaction);

    PageResult<Transaction> findByClientId(String clientId, int page, int size);
}
