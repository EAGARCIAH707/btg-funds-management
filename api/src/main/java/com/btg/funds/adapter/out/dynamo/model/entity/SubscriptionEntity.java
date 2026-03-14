package com.btg.funds.adapter.out.dynamo.model.entity;

import com.btg.funds.domain.model.Subscription;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.math.BigDecimal;
import java.time.Instant;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class SubscriptionEntity {

    private String clientId;
    private String fundId;
    private String fundName;
    private BigDecimal amount;
    private Instant subscribedAt;

    @DynamoDbPartitionKey
    public String getClientId() { return clientId; }

    @DynamoDbSortKey
    public String getFundId() { return fundId; }

    public Subscription toDomain() {
        return new Subscription(clientId, fundId, fundName, amount, subscribedAt);
    }

    public static SubscriptionEntity fromDomain(Subscription sub) {
        var entity = new SubscriptionEntity();
        entity.setClientId(sub.clientId());
        entity.setFundId(sub.fundId());
        entity.setFundName(sub.fundName());
        entity.setAmount(sub.amount());
        entity.setSubscribedAt(sub.subscribedAt());
        return entity;
    }
}
