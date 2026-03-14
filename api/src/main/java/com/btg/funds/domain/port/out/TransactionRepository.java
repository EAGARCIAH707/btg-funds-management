package com.btg.funds.domain.port.out;

import com.btg.funds.domain.model.Transaction;

import java.util.List;

public interface TransactionRepository {

    void save(Transaction transaction);

    List<Transaction> findByClientId(String clientId);
}
