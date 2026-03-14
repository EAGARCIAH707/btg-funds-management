package com.btg.funds.adapter.out.dynamo.model.entity;

import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.TransactionType;
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
public class TransactionEntity {

    private String clientId;
    private String transactionId;
    private String fundId;
    private String fundName;
    private String type;
    private BigDecimal amount;
    private Instant timestamp;

    @DynamoDbPartitionKey
    public String getClientId() { return clientId; }

    @DynamoDbSortKey
    public String getTransactionId() { return transactionId; }

    public Transaction toDomain() {
        return new Transaction(transactionId, clientId, fundId, fundName,
                TransactionType.valueOf(type), amount, timestamp);
    }

    public static TransactionEntity fromDomain(Transaction tx) {
        var entity = new TransactionEntity();
        entity.setClientId(tx.clientId());
        entity.setTransactionId(tx.transactionId());
        entity.setFundId(tx.fundId());
        entity.setFundName(tx.fundName());
        entity.setType(tx.type().name());
        entity.setAmount(tx.amount());
        entity.setTimestamp(tx.timestamp());
        return entity;
    }
}
