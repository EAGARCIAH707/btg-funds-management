package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.CancelSubscriptionUseCase;
import com.btg.funds.domain.port.out.*;

import java.time.Instant;
import java.util.UUID;

public class CancelSubscriptionUseCaseImpl implements CancelSubscriptionUseCase {

    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationPort notificationPort;

    public CancelSubscriptionUseCaseImpl(ClientRepository clientRepository,
                                         FundRepository fundRepository,
                                         SubscriptionRepository subscriptionRepository,
                                         TransactionRepository transactionRepository,
                                         NotificationPort notificationPort) {
        this.clientRepository = clientRepository;
        this.fundRepository = fundRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.transactionRepository = transactionRepository;
        this.notificationPort = notificationPort;
    }

    @Override
    public Transaction execute(Command command) {
        var client = clientRepository.findById(command.clientId())
                .orElseThrow(() -> new ClientNotFoundException(command.clientId()));

        var fund = fundRepository.findById(command.fundId())
                .orElseThrow(() -> new FundNotFoundException(command.fundId()));

        var subscription = subscriptionRepository.findByClientAndFund(command.clientId(), command.fundId())
                .orElseThrow(() -> new SubscriptionNotFoundException(command.clientId(), command.fundId()));

        var updatedClient = client.credit(subscription.amount());
        clientRepository.save(updatedClient);

        subscriptionRepository.delete(command.clientId(), command.fundId());

        var transaction = new Transaction(
                UUID.randomUUID().toString(),
                client.id(),
                fund.id(),
                fund.name(),
                TransactionType.CANCELACION,
                subscription.amount(),
                Instant.now()
        );
        transactionRepository.save(transaction);

        notificationPort.notify(updatedClient, fund, TransactionType.CANCELACION);

        return transaction;
    }
}
