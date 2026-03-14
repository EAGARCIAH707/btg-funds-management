package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.domain.model.Subscription;
import com.btg.funds.domain.port.out.SubscriptionRepository;
import com.btg.funds.adapter.out.dynamo.model.entity.SubscriptionEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

public class SubscriptionDynamoAdapter implements SubscriptionRepository {

    private final DynamoDbTable<SubscriptionEntity> table;

    public SubscriptionDynamoAdapter(DynamoDbTable<SubscriptionEntity> table) {
        this.table = table;
    }

    @Override
    public Optional<Subscription> findByClientAndFund(String clientId, String fundId) {
        var key = Key.builder().partitionValue(clientId).sortValue(fundId).build();
        var entity = table.getItem(key);
        return Optional.ofNullable(entity).map(SubscriptionEntity::toDomain);
    }

    @Override
    public void save(Subscription subscription) {
        table.putItem(SubscriptionEntity.fromDomain(subscription));
    }

    @Override
    public void delete(String clientId, String fundId) {
        var key = Key.builder().partitionValue(clientId).sortValue(fundId).build();
        table.deleteItem(key);
    }
}
