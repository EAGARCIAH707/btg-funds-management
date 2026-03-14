package com.btg.funds.configuration;

import com.btg.funds.domain.usecase.*;
import com.btg.funds.domain.port.in.*;
import com.btg.funds.domain.port.out.*;
import com.btg.funds.adapter.out.dynamo.*;
import com.btg.funds.adapter.out.dynamo.model.entity.*;
import com.btg.funds.adapter.out.sns.SnsNotificationAdapter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.services.sns.SnsClient;

@Configuration
public class BeanConfig {

    // --- Out Ports (adapters) ---

    @Bean
    public ClientRepository clientRepository(DynamoDbTable<ClientEntity> table) {
        return new ClientDynamoAdapter(table);
    }

    @Bean
    public FundRepository fundRepository(DynamoDbTable<FundEntity> table) {
        return new FundDynamoAdapter(table);
    }

    @Bean
    public SubscriptionRepository subscriptionRepository(DynamoDbTable<SubscriptionEntity> table) {
        return new SubscriptionDynamoAdapter(table);
    }

    @Bean
    public TransactionRepository transactionRepository(DynamoDbTable<TransactionEntity> table) {
        return new TransactionDynamoAdapter(table);
    }

    @Bean
    public NotificationPort notificationPort(SnsClient snsClient,
                                             @Value("${aws.sns.topic-arn}") String topicArn) {
        return new SnsNotificationAdapter(snsClient, topicArn);
    }

    // --- In Ports (use cases) ---

    @Bean
    public SubscribeToFundUseCase subscribeToFundUseCase(ClientRepository clientRepo,
                                                         FundRepository fundRepo,
                                                         SubscriptionRepository subRepo,
                                                         TransactionRepository txRepo,
                                                         NotificationPort notificationPort) {
        return new SubscribeToFundUseCaseImpl(clientRepo, fundRepo, subRepo, txRepo, notificationPort);
    }

    @Bean
    public CancelSubscriptionUseCase cancelSubscriptionUseCase(ClientRepository clientRepo,
                                                               FundRepository fundRepo,
                                                               SubscriptionRepository subRepo,
                                                               TransactionRepository txRepo,
                                                               NotificationPort notificationPort) {
        return new CancelSubscriptionUseCaseImpl(clientRepo, fundRepo, subRepo, txRepo, notificationPort);
    }

    @Bean
    public GetTransactionHistoryUseCase getTransactionHistoryUseCase(ClientRepository clientRepo,
                                                                     TransactionRepository txRepo) {
        return new GetTransactionHistoryUseCaseImpl(clientRepo, txRepo);
    }
}
