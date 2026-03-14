package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.adapter.out.dynamo.model.entity.SubscriptionEntity;
import com.btg.funds.domain.model.Subscription;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionDynamoAdapterTest {

    @Mock
    private DynamoDbTable<SubscriptionEntity> table;

    private SubscriptionDynamoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new SubscriptionDynamoAdapter(table);
    }

    @Test
    void findByClientAndFund_shouldReturnSubscription_whenExists() {
        var entity = new SubscriptionEntity();
        entity.setClientId("client-001");
        entity.setFundId("1");
        entity.setFundName("FPV_BTG");
        entity.setAmount(new BigDecimal("75000"));
        entity.setSubscribedAt(Instant.parse("2026-03-14T10:00:00Z"));

        when(table.getItem(any(Key.class))).thenReturn(entity);

        var result = adapter.findByClientAndFund("client-001", "1");

        assertThat(result).isPresent();
        assertThat(result.get().clientId()).isEqualTo("client-001");
        assertThat(result.get().fundId()).isEqualTo("1");
    }

    @Test
    void findByClientAndFund_shouldReturnEmpty_whenNotFound() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        var result = adapter.findByClientAndFund("client-001", "99");

        assertThat(result).isEmpty();
    }

    @Test
    void save_shouldPutMappedEntity() {
        var subscription = new Subscription("client-001", "1", "FPV_BTG",
                new BigDecimal("75000"), Instant.parse("2026-03-14T10:00:00Z"));

        adapter.save(subscription);

        var captor = ArgumentCaptor.forClass(SubscriptionEntity.class);
        verify(table).putItem(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getClientId()).isEqualTo("client-001");
        assertThat(saved.getFundId()).isEqualTo("1");
        assertThat(saved.getFundName()).isEqualTo("FPV_BTG");
    }

    @Test
    void delete_shouldDeleteByCompositeKey() {
        adapter.delete("client-001", "1");

        verify(table).deleteItem(any(Key.class));
    }
}
