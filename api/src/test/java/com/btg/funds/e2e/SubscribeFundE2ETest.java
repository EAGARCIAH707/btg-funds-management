package com.btg.funds.e2e;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.Fund;
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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SubscribeFundE2ETest {

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
    private static final BigDecimal FUND_MINIMUM = new BigDecimal("75000");
    private static final BigDecimal CLIENT_BALANCE = new BigDecimal("500000");

    private Client client;
    private Fund fund;

    @BeforeEach
    void setUp() {
        client = new Client(CLIENT_ID, "Juan Pérez", "juan@email.com", "+573001234567",
                CLIENT_BALANCE, NotificationPreference.EMAIL);
        fund = new Fund(FUND_ID, FUND_NAME, FUND_MINIMUM, "FPV");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - suscripción exitosa retorna 201")
    void subscribeSuccess() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund(CLIENT_ID, FUND_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("fundId")).isEqualTo(FUND_ID);
        assertThat(response.getBody().get("fundName")).isEqualTo(FUND_NAME);
        assertThat(response.getBody().get("type")).isEqualTo("APERTURA");
        assertThat(new BigDecimal(response.getBody().get("amount").toString())).isEqualByComparingTo(FUND_MINIMUM);
        assertThat(response.getBody().get("transactionId")).isNotNull();

        verify(clientRepository).save(any(Client.class));
        verify(subscriptionRepository).save(any());
        verify(transactionRepository).save(any(Transaction.class));
        verify(notificationPort).notify(any(Client.class), eq(fund), eq(TransactionType.APERTURA));
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - cliente no encontrado retorna 404")
    void subscribeClientNotFound() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - fondo no encontrado retorna 404")
    void subscribeFundNotFound() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - saldo insuficiente retorna 400")
    void subscribeInsufficientBalance() {
        var poorClient = new Client(CLIENT_ID, "Juan Pérez", "juan@email.com", "+573001234567",
                new BigDecimal("10000"), NotificationPreference.EMAIL);
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(poorClient));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund(CLIENT_ID, FUND_ID)).thenReturn(Optional.empty());

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("INSUFFICIENT_BALANCE");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - ya suscrito retorna 409")
    void subscribeAlreadySubscribed() {
        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(fundRepository.findById(FUND_ID)).thenReturn(Optional.of(fund));
        when(subscriptionRepository.findByClientAndFund(CLIENT_ID, FUND_ID))
                .thenReturn(Optional.of(new com.btg.funds.domain.model.Subscription(
                        CLIENT_ID, FUND_ID, FUND_NAME, FUND_MINIMUM, java.time.Instant.now())));

        var request = Map.of("clientId", CLIENT_ID, "fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().get("error")).isEqualTo("ALREADY_SUBSCRIBED");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - request sin clientId retorna 400 validación")
    void subscribeValidationError() {
        var request = Map.of("fundId", FUND_ID);
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    @DisplayName("POST /api/v1/funds/subscribe - request vacío retorna 400 validación")
    void subscribeEmptyBody() {
        var request = Map.of("clientId", "", "fundId", "");
        ResponseEntity<Map> response = restTemplate.postForEntity("/api/v1/funds/subscribe", request, Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo("VALIDATION_ERROR");
    }
}
