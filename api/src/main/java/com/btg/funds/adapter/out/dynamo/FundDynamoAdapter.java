package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.port.out.FundRepository;
import com.btg.funds.adapter.out.dynamo.model.entity.FundEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

public class FundDynamoAdapter implements FundRepository {

    private final DynamoDbTable<FundEntity> table;

    public FundDynamoAdapter(DynamoDbTable<FundEntity> table) {
        this.table = table;
    }

    @Override
    public Optional<Fund> findById(String fundId) {
        var key = Key.builder().partitionValue(fundId).build();
        var entity = table.getItem(key);
        return Optional.ofNullable(entity).map(FundEntity::toDomain);
    }
}
