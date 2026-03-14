package com.btg.funds.adapter.out.dynamo.model.entity;

import com.btg.funds.domain.model.Fund;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.math.BigDecimal;

@DynamoDbBean
@Getter
@Setter
@NoArgsConstructor
public class FundEntity {

    private String id;
    private String name;
    private BigDecimal minimumAmount;
    private String category;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    public Fund toDomain() {
        return new Fund(id, name, minimumAmount, category);
    }
}
