package com.btg.funds.adapter.out.dynamo;

import com.btg.funds.adapter.out.dynamo.model.entity.FundEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundDynamoAdapterTest {

    @Mock
    private DynamoDbTable<FundEntity> table;

    private FundDynamoAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new FundDynamoAdapter(table);
    }

    @Test
    void findById_shouldReturnFund_whenEntityExists() {
        var entity = new FundEntity();
        entity.setId("1");
        entity.setName("FPV_BTG");
        entity.setMinimumAmount(new BigDecimal("75000"));
        entity.setCategory("FPV");

        when(table.getItem(any(Key.class))).thenReturn(entity);

        var result = adapter.findById("1");

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo("1");
        assertThat(result.get().name()).isEqualTo("FPV_BTG");
        assertThat(result.get().minimumAmount()).isEqualByComparingTo(new BigDecimal("75000"));
    }

    @Test
    void findById_shouldReturnEmpty_whenEntityNotFound() {
        when(table.getItem(any(Key.class))).thenReturn(null);

        var result = adapter.findById("unknown");

        assertThat(result).isEmpty();
    }
}
