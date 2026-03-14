package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.*;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.CancelSubscriptionUseCase;
import com.btg.funds.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelSubscriptionUseCaseImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private FundRepository fundRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationPort notificationPort;

    private CancelSubscriptionUseCaseImpl useCase;

    private static final Client CLIENT = new Client(
            "client-001", "Juan Pérez", "juan@email.com", "+573001234567",
            new BigDecimal("425000"), NotificationPreference.EMAIL
    );

    private static final Fund FUND = new Fund(
            "1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV"
    );

    private static final Subscription SUBSCRIPTION = new Subscription(
            "client-001", "1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), Instant.now()
    );

    @BeforeEach
    void setUp() {
        useCase = new CancelSubscriptionUseCaseImpl(
                clientRepository, fundRepository, subscriptionRepository,
                transactionRepository, notificationPort
        );
    }

    @Test
    void shouldCancelSuccessfully() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(CLIENT));
        when(fundRepository.findById("1")).thenReturn(Optional.of(FUND));
        when(subscriptionRepository.findByClientAndFund("client-001", "1"))
                .thenReturn(Optional.of(SUBSCRIPTION));

        var result = useCase.execute(new CancelSubscriptionUseCase.Command("client-001", "1"));

        assertThat(result.type()).isEqualTo(TransactionType.CANCELACION);
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("75000"));

        verify(clientRepository).save(argThat(c ->
                c.balance().compareTo(new BigDecimal("500000")) == 0));
        verify(subscriptionRepository).delete("client-001", "1");
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationPort).notify(any(Client.class), eq(FUND), eq(TransactionType.CANCELACION));
    }

    @Test
    void shouldThrowWhenNotSubscribed() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(CLIENT));
        when(fundRepository.findById("1")).thenReturn(Optional.of(FUND));
        when(subscriptionRepository.findByClientAndFund("client-001", "1")).thenReturn(Optional.empty());

        var command = new CancelSubscriptionUseCase.Command("client-001", "1");
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(SubscriptionNotFoundException.class);
    }
}
