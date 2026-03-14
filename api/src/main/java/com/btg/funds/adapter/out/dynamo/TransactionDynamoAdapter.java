package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.domain.model.PageResult;
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
    public PageResult<Transaction> findByClientId(String clientId, int page, int size) {
        var condition = QueryConditional.keyEqualTo(
                Key.builder().partitionValue(clientId).build()
        );

        List<Transaction> allItems = table.query(condition).items().stream()
                .map(TransactionEntity::toDomain)
                .toList();

        int totalElements = allItems.size();
        int totalPages = (int) Math.ceil((double) totalElements / size);
        int fromIndex = Math.min(page * size, totalElements);
        int toIndex = Math.min(fromIndex + size, totalElements);

        return new PageResult<>(allItems.subList(fromIndex, toIndex), page, size, totalElements, totalPages);
    }
}
