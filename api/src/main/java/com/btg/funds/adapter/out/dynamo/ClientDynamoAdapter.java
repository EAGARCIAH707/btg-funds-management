package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.port.out.ClientRepository;
import com.btg.funds.adapter.out.dynamo.model.entity.ClientEntity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.util.Optional;

public class ClientDynamoAdapter implements ClientRepository {

    private final DynamoDbTable<ClientEntity> table;

    public ClientDynamoAdapter(DynamoDbTable<ClientEntity> table) {
        this.table = table;
    }

    @Override
    public Optional<Client> findById(String clientId) {
        var key = Key.builder().partitionValue(clientId).build();
        var entity = table.getItem(key);
        return Optional.ofNullable(entity).map(ClientEntity::toDomain);
    }

    @Override
    public void save(Client client) {
        table.putItem(ClientEntity.fromDomain(client));
    }
}
