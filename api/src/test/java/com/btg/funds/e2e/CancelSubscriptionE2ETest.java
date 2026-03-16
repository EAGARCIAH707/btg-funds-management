package com.btg.funds.e2e;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.Fund;
import com.btg.funds.domain.model.Subscription;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.out.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CancelSubscriptionE2ETest {

    @Autowired
    private TestRestTemplate restTemplate;

    @MockitoBean
    private ClientRepository clientRepository;

    @MockitoBean
    private FundRepository fundRepository;

    @MockitoBean
    private SubscriptionRepository subscriptionRepository;

    @MockitoBean
    private TransactionRepository transactionRepository;

    @MockitoBean
    private NotificationPort notificationPort;

    private static final String CLIENT_ID = "client-001";
    private static final String FUND_ID = "fund-001";
    private static final String FUND_NAME = "FPV_BTG_PACTUAL_RECAUDADORA";
    private static final BigDecimal SUBSCRIPTION_AMOUNT = new BigDecimal("75000");

    private Client client;
    private Fund fund;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        client = new Client(CLIENT_ID, "Juan Pérez", "juan@email.com", "+573001234567",
                new BigDecimal("425000"), NotificationPreference.SMS);
        fund = new Fund(FUND_ID, FUND_NAME, SUBSCRIPTION_AMOUNT, "FPV");
        subscription = new Subscription(CLIENT_ID, FUND_ID, FUND_NAME, SUBSCRIPTION_AMOUNT, Instant.now());
    }

    @Test
    @DisplayName("POST /api/v1/funds/cancel - cancelación exitosa retorna 200")
    void cancelSuccess() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund(CLIENT_ID, FUND_ID)).thenReturn(Optional.of(subscription));

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/cancel", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("fundId")).isEqualTo(FUND_ID);
        assertThat(response.getBody().get("fundName")).isEqualTo(FUND_NAME);
        assertThat(response.getBody().get("type")).isEqualTo("CANCELACION");
        assertThat(new BigDecimal(response.getBody().get("amount").toString())).isEqualByComparingTo(SUBSCRIPTION_AMOUNT);

        verify(clientRepository).save(any(Client.class));
        verify(subscriptionRepository).delete(CLIENT_ID, FUND_ID);
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationPort).notify(any(Client.class), eq(fund), eq(TransactionType.CANCELACION));
    }

    @Test
    @DisplayName("POST /api/v1/funds/cancel - cliente no encontrado retorna 404")
    void cancelClientNotFound() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/cancel", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("POST /api/v1/funds/cancel - suscripción no encontrada retorna 404")
    void cancelSubscriptionNotFound() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund(CLIENT_ID, FUND_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/cancel", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("POST /api/v1/funds/cancel - request sin fundId retorna 400 validación")
    void cancelValidationError() {
        var request = Map.of("clientId", CLIENT_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/cancel", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_ERROR");
    }
}
