package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.port.out.TransactionRepository;
import com.btg.funds.adapter.out.dynamo.model.entity.TransactionEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.util.List;

public class TransactionDynamoAdapter implements TransactionRepository {

    private final DynamoDbTable<TransactionEntity> table;

    public TransactionDynamoAdapter(DynamoDbTable<TransactionEntity> table) {
        this.table = table;
    }

    @Override
    public void save(Transaction transaction) {
        table.putItem(TransactionEntity.fromDomain(transaction));
    }

    @Override
    public List<Transaction> findByClientId(String clientId) {
        var condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clientId).build()
        );
        return table.query(condition).items().stream()
                .map(TransactionEntity::toDomain)
                .toList();
    }
}
