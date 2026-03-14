package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.AlreadySubscribedException;
import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.model.*;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.SubscribeToFundUseCase;
import com.btg.funds.domain.port.out.*;

import java.time.Instant;
import java.util.UUID;

public class SubscribeToFundUseCaseImpl implements SubscribeToFundUseCase {

    private final ClientRepository clientRepository;
    private final FundRepository fundRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;
    private final NotificationPort notificationPort;

    public SubscribeToFundUseCaseImpl(ClientRepository clientRepository,
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

        subscriptionRepository.findByClientAndFund(command.clientId(), command.fundId())
                .ifPresent(existing -> { throw new AlreadySubscribedException(fund.name()); });

        if (!client.hasEnoughBalance(fund.minimumAmount())) {
            throw new InsufficientBalanceException(fund.name());
        }

        var updatedClient = client.debit(fund.minimumAmount());
        clientRepository.save(updatedClient);

        var subscription = new Subscription(
                client.id(), fund.id(), fund.name(), fund.minimumAmount(), Instant.now()
        );
        subscriptionRepository.save(subscription);

        var transaction = new Transaction(
                UUID.randomUUID().toString(),
                client.id(),
                fund.id(),
                fund.name(),
                TransactionType.APERTURA,
                fund.minimumAmount(),
                Instant.now()
        );
        transactionRepository.save(transaction);

        notificationPort.notify(updatedClient, fund, TransactionType.APERTURA);

        return transaction;
    }
}
