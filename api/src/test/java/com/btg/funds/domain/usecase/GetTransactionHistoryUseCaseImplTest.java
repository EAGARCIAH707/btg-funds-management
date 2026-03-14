package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.model.*;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.out.ClientRepository;
import com.btg.funds.domain.port.out.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTransactionHistoryUseCaseImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private TransactionRepository transactionRepository;

    private GetTransactionHistoryUseCaseImpl useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetTransactionHistoryUseCaseImpl(clientRepository, transactionRepository);
    }

    @Test
    void shouldReturnTransactionHistory() {
        var client = new Client("client-001", "Juan", "j@e.com", "+57300",
                new BigDecimal("500000"), NotificationPreference.EMAIL);
        var transactions = List.of(
                new Transaction("tx-1", "client-001", "1", "FPV_BTG_PACTUAL_RECAUDADORA",
                        TransactionType.APERTURA, new BigDecimal("75000"), Instant.now()),
                new Transaction("tx-2", "client-001", "1", "FPV_BTG_PACTUAL_RECAUDADORA",
                        TransactionType.CANCELACION, new BigDecimal("75000"), Instant.now())
        );

        when(clientRepository.findById("client-001")).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId("client-001")).thenReturn(transactions);

        var result = useCase.execute("client-001");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).type()).isEqualTo(TransactionType.APERTURA);
        assertThat(result.get(1).type()).isEqualTo(TransactionType.CANCELACION);
    }

    @Test
    void shouldThrowWhenClientNotFound() {
        when(clientRepository.findById("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute("unknown"))
                .isInstanceOf(ClientNotFoundException.class);
    }
}
