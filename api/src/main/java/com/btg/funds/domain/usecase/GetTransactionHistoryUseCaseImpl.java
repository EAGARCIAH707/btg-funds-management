package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.port.in.GetTransactionHistoryUseCase;
import com.btg.funds.domain.port.out.ClientRepository;
import com.btg.funds.domain.port.out.TransactionRepository;

import java.util.List;

public class GetTransactionHistoryUseCaseImpl implements GetTransactionHistoryUseCase {

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    public GetTransactionHistoryUseCaseImpl(ClientRepository clientRepository,
                                            TransactionRepository transactionRepository) {
        this.clientRepository = clientRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    public List<Transaction> execute(String clientId) {
        clientRepository.findById(clientId)
                .orElseThrow(() -> new ClientNotFoundException(clientId));

        return transactionRepository.findByClientId(clientId);
    }
}
