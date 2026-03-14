package com.btg.funds.configuration;

import com.btg.funds.adapter.out.dynamo.model.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.sns.SnsClient;

import java.net.URI;

@Configuration
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Value("${aws.endpoint:#{null}}")
    private String endpoint;

    @Value("${aws.dynamodb.table-prefix:}")
    private String tablePrefix;

    @Bean
    public DynamoDbClient dynamoDbClient() {
        var builder = DynamoDbClient.builder().region(Region.of(region));
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    public DynamoDbEnhancedClient dynamoDbEnhancedClient(DynamoDbClient dynamoDbClient) {
        return DynamoDbEnhancedClient.builder().dynamoDbClient(dynamoDbClient).build();
    }

    @Bean
    public SnsClient snsClient() {
        var builder = SnsClient.builder().region(Region.of(region));
        if (endpoint != null) {
            builder.endpointOverride(URI.create(endpoint))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create("test", "test")));
        } else {
            builder.credentialsProvider(DefaultCredentialsProvider.create());
        }
        return builder.build();
    }

    @Bean
    public DynamoDbTable<ClientEntity> clientsTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + "clients", TableSchema.fromBean(ClientEntity.class));
    }

    @Bean
    public DynamoDbTable<FundEntity> fundsTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + "funds", TableSchema.fromBean(FundEntity.class));
    }

    @Bean
    public DynamoDbTable<SubscriptionEntity> subscriptionsTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + "subscriptions", TableSchema.fromBean(SubscriptionEntity.class));
    }

    @Bean
    public DynamoDbTable<TransactionEntity> transactionsTable(DynamoDbEnhancedClient client) {
        return client.table(tablePrefix + "transactions", TableSchema.fromBean(TransactionEntity.class));
    }
}
