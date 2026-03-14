package com.btg.funds.adapter.out.dynamo.model.entity;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.enums.NotificationPreference;
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
public class ClientEntity {

    private String id;
    private String name;
    private String email;
    private String phone;
    private BigDecimal balance;
    private String notificationPreference;

    @DynamoDbPartitionKey
    public String getId() { return id; }

    public Client toDomain() {
        return new Client(id, name, email, phone, balance,
                NotificationPreference.valueOf(notificationPreference));
    }

    public static ClientEntity fromDomain(Client client) {
        var entity = new ClientEntity();
        entity.setId(client.id());
        entity.setName(client.name());
        entity.setEmail(client.email());
        entity.setPhone(client.phone());
        entity.setBalance(client.balance());
        entity.setNotificationPreference(client.notificationPreference().name());
        return entity;
    }
}
