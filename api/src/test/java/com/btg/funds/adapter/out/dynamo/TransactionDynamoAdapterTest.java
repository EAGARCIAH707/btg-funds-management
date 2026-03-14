package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.adapter.out.dynamo.model.entity.TransactionEntity;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.TransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.pagination.sync.SdkIterable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.model.PageIterable;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionDynamoAdapterTest {

    @Mock
    private DynamoDbTable<TransactionEntity> table;

    @Mock
    private PageIterable<TransactionEntity> pageIterable;

    @Mock
    private SdkIterable<TransactionEntity> sdkIterable;

    private TransactionDynamoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new TransactionDynamoAdapter(table);
    }

    @Test
    void save_shouldPutMappedEntity() {
        var transaction = new Transaction("tx-001", "client-001", "1", "FPV_BTG",
                TransactionType.APERTURA, new BigDecimal("75000"), Instant.parse("2026-03-14T10:00:00Z"));

        adapter.save(transaction);

        var captor = ArgumentCaptor.forClass(TransactionEntity.class);
        verify(table).putItem(captor.capture());

        var saved = captor.getValue();
        assertThat(saved.getTransactionId()).isEqualTo("tx-001");
        assertThat(saved.getClientId()).isEqualTo("client-001");
        assertThat(saved.getType()).isEqualTo("APERTURA");
    }

    @Test
    void findByClientId_shouldReturnPaginatedResults() {
        var entity1 = createEntity("tx-1", TransactionType.APERTURA);
        var entity2 = createEntity("tx-2", TransactionType.CANCELACION);
        var entity3 = createEntity("tx-3", TransactionType.APERTURA);

        when(table.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(entity1, entity2, entity3));

        var result = adapter.findByClientId("client-001", 0, 2);

        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(3);
        assertThat(result.totalPages()).isEqualTo(2);
        assertThat(result.page()).isZero();
    }

    @Test
    void findByClientId_shouldReturnSecondPage() {
        var entity1 = createEntity("tx-1", TransactionType.APERTURA);
        var entity2 = createEntity("tx-2", TransactionType.CANCELACION);
        var entity3 = createEntity("tx-3", TransactionType.APERTURA);

        when(table.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.of(entity1, entity2, entity3));

        var result = adapter.findByClientId("client-001", 1, 2);

        assertThat(result.content()).hasSize(1);
        assertThat(result.page()).isEqualTo(1);
    }

    @Test
    void findByClientId_shouldReturnEmptyPage_whenNoResults() {
        when(table.query(any(QueryConditional.class))).thenReturn(pageIterable);
        when(pageIterable.items()).thenReturn(sdkIterable);
        when(sdkIterable.stream()).thenReturn(Stream.empty());

        var result = adapter.findByClientId("client-001", 0, 10);

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
        assertThat(result.totalPages()).isZero();
    }

    private TransactionEntity createEntity(String txId, TransactionType type) {
        var entity = new TransactionEntity();
        entity.setClientId("client-001");
        entity.setTransactionId(txId);
        entity.setFundId("1");
        entity.setFundName("FPV_BTG");
        entity.setType(type.name());
        entity.setAmount(new BigDecimal("75000"));
        entity.setTimestamp(Instant.now());
        return entity;
    }
}
