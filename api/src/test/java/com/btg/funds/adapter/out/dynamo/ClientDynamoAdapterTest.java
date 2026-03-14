package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.adapter.out.dynamo.model.entity.ClientEntity;
import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.enums.NotificationPreference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientDynamoAdapterTest {

    @Mock
    private DynamoDbTable<ClientEntity> table;

    private ClientDynamoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new ClientDynamoAdapter(table);
    }

    @Test
    void findById_shouldReturnClient_whenEntityExists() {
        var entity = new ClientEntity();
        entity.setId("client-001");
        entity.setName("Juan");
        entity.setEmail("juan@email.com");
        entity.setPhone("+573001234567");
        entity.setBalance(new BigDecimal("500000"));
        entity.setNotificationPreference("EMAIL");

        when(table.getItem(any(Key.class))).thenReturn(entity);

        var result = adapter.findById("client-001");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("client-001");
        assertThat(result.get().name()).isEqualTo("Juan");
        assertThat(result.get().notificationPreference()).isEqualTo(NotificationPreference.EMAIL);
    }

    @Test
    void findById_shouldReturnEmpty_whenEntityNotFound() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        var result = adapter.findById("unknown");

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldPutMappedEntity() {
        var client = new Client("client-001", "Juan", "juan@email.com", "+573001234567",
                new BigDecimal("500000"), NotificationPreference.EMAIL);

        adapter.save(client);

        var captor = ArgumentCaptor.forClass(ClientEntity.class);
        verify(table).putItem(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo("client-001");
        assertThat(saved.getBalance()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(saved.getNotificationPreference()).isEqualTo("EMAIL");
    }
}
