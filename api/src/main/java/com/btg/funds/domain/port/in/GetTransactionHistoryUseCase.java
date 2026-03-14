package com.btg.funds.domain.port.in;

import com.btg.funds.domain.model.PageResult;
import com.btg.funds.domain.model.Transaction;

public interface GetTransactionHistoryUseCase {

    PageResult<Transaction> execute(String clientId, int page, int size);
}
