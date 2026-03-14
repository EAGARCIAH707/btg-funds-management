package com.btg.funds.domain.usecase;

import com.btg.funds.domain.exception.AlreadySubscribedException;
import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.model.*;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.SubscribeToFundUseCase;
import com.btg.funds.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscribeToFundUseCaseImplTest {

    @Mock private ClientRepository clientRepository;
    @Mock private FundRepository fundRepository;
    @Mock private SubscriptionRepository subscriptionRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private NotificationPort notificationPort;

    private SubscribeToFundUseCaseImpl useCase;

    private static final Client CLIENT = new Client(
            "client-001", "Juan Pérez", "juan@email.com", "+573001234567",
            new BigDecimal("500000"), NotificationPreference.EMAIL
    );

    private static final Fund FUND = new Fund(
            "1", "FPV_BTG_PACTUAL_RECAUDADORA", new BigDecimal("75000"), "FPV"
    );

    @BeforeEach
    void setUp() {
        useCase = new SubscribeToFundUseCaseImpl(
                clientRepository, fundRepository, subscriptionRepository,
                transactionRepository, notificationPort
        );
    }

    @Test
    void shouldSubscribeSuccessfully() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(CLIENT));
        when(fundRepository.findById("1")).thenReturn(Optional.of(FUND));
        when(subscriptionRepository.findByClientAndFund("client-001", "1")).thenReturn(Optional.empty());

        var result = useCase.execute(new SubscribeToFundUseCase.Command("client-001", "1"));

        assertThat(result.type()).isEqualTo(TransactionType.APERTURA);
        assertThat(result.fundId()).isEqualTo("1");
        assertThat(result.amount()).isEqualByComparingTo(new BigDecimal("75000"));

        verify(clientRepository).save(argThat(c ->
                c.balance().compareTo(new BigDecimal("425000")) == 0));
        verify(subscriptionRepository).save(any(Subscription.class));
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationPort).notify(any(Client.class), eq(FUND), eq(TransactionType.APERTURA));
    }

    @Test
    void shouldThrowWhenClientNotFound() {
        when(clientRepository.findById("unknown")).thenReturn(Optional.empty());

        var command = new SubscribeToFundUseCase.Command("unknown", "1");
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(ClientNotFoundException.class);
    }

    @Test
    void shouldThrowWhenFundNotFound() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(CLIENT));
        when(fundRepository.findById("99")).thenReturn(Optional.empty());

        var command = new SubscribeToFundUseCase.Command("client-001", "99");
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(FundNotFoundException.class);
    }

    @Test
    void shouldThrowWhenInsufficientBalance() {
        var brokeCient = new Client(
                "client-002", "Sin Plata", "broke@email.com", "+573009999999",
                new BigDecimal("10000"), NotificationPreference.SMS
        );
        var expensiveFund = new Fund("4", "FDO-ACCIONES", new BigDecimal("250000"), "FIC");

        when(clientRepository.findById("client-002")).thenReturn(Optional.of(brokeCient));
        when(fundRepository.findById("4")).thenReturn(Optional.of(expensiveFund));
        when(subscriptionRepository.findByClientAndFund("client-002", "4")).thenReturn(Optional.empty());

        var command = new SubscribeToFundUseCase.Command("client-002", "4");
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("FDO-ACCIONES");
    }

    @Test
    void shouldThrowWhenAlreadySubscribed() {
        when(clientRepository.findById("client-001")).thenReturn(Optional.of(CLIENT));
        when(fundRepository.findById("1")).thenReturn(Optional.of(FUND));
        when(subscriptionRepository.findByClientAndFund("client-001", "1"))
                .thenReturn(Optional.of(new Subscription("client-001", "1", FUND.name(),
                        FUND.minimumAmount(), java.time.Instant.now())));

        var command = new SubscribeToFundUseCase.Command("client-001", "1");
        assertThatThrownBy(() -> useCase.execute(command))
                .isInstanceOf(AlreadySubscribedException.class);
    }
}
