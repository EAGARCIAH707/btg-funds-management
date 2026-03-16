package com.btg.funds.e2e;

import com.btg.funds.domain.model.*;
import com.btg.funds.domain.model.enums.NotificationPreference;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.out.*;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Flujo completo E2E: suscribir → consultar historial → cancelar → consultar historial")
class FullFlowE2ETest {

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

    @Test
    @DisplayName("Flujo completo: suscripción → historial → cancelación → historial actualizado")
    void fullSubscriptionLifecycle() {
        var clientId = "client-100";
        var fundId = "fund-200";
        var fundName = "FPV_BTG_PACTUAL_RECAUDADORA";
        var minimumAmount = new BigDecimal("75000");
        var initialBalance = new BigDecimal("500000");

        var client = new Client(clientId, "María García", "maria@email.com", "+573009876543",
                initialBalance, NotificationPreference.EMAIL);
        var fund = new Fund(fundId, fundName, minimumAmount, "FPV");

        var capturedClient = new AtomicReference<>(client);
        var capturedSubscription = new AtomicReference<Subscription>(null);
        var capturedTransaction = new AtomicReference<Transaction>(null);

        // Simular persistencia en memoria
        when(clientRepository.findById(clientId)).thenAnswer(inv -> Optional.of(capturedClient.get()));
        doAnswer(inv -> { capturedClient.set(inv.getArgument(0)); return null; }).when(clientRepository).save(any());

        when(fundRepository.findById(fundId)).thenReturn(Optional.of(fund));

        when(subscriptionRepository.findByClientAndFund(clientId, fundId))
                .thenAnswer(inv -> Optional.ofNullable(capturedSubscription.get()));
        doAnswer(inv -> { capturedSubscription.set(inv.getArgument(0)); return null; }).when(subscriptionRepository).save(any());
        doAnswer(inv -> { capturedSubscription.set(null); return null; }).when(subscriptionRepository).delete(clientId, fundId);

        doAnswer(inv -> { capturedTransaction.set(inv.getArgument(0)); return null; }).when(transactionRepository).save(any());

        // === PASO 1: Suscribir al fondo ===
        var subscribeRequest = Map.of("clientId", clientId, "fundId", fundId);
        ResponseEntity<Map> subscribeResponse = restTemplate.postForEntity(
                "/api/v1/funds/subscribe", subscribeRequest, Map.class);

        assertThat(subscribeResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(subscribeResponse.getBody().get("type")).isEqualTo("APERTURA");
        assertThat(subscribeResponse.getBody().get("fundName")).isEqualTo(fundName);

        // Verificar que el balance se debito
        assertThat(capturedClient.get().balance()).isEqualByComparingTo(initialBalance.subtract(minimumAmount));

        // === PASO 2: Consultar historial post-suscripción ===
        var txAfterSubscribe = capturedTransaction.get();
        when(transactionRepository.findByClientId(clientId, 0, 10))
                .thenReturn(new PageResult<>(List.of(txAfterSubscribe), 0, 10, 1, 1));

        ResponseEntity<Map> historyResponse1 = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}", Map.class, clientId);

        assertThat(historyResponse1.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content1 = (List<Map<String, Object>>) historyResponse1.getBody().get("content");
        assertThat(content1).hasSize(1);
        assertThat(content1.get(0).get("type")).isEqualTo("APERTURA");

        // === PASO 3: Intentar suscribirse de nuevo (debe fallar 409) ===
        ResponseEntity<Map> duplicateResponse = restTemplate.postForEntity(
                "/api/v1/funds/subscribe", subscribeRequest, Map.class);
        assertThat(duplicateResponse.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

        // === PASO 4: Cancelar suscripción ===
        var cancelRequest = Map.of("clientId", clientId, "fundId", fundId);
        ResponseEntity<Map> cancelResponse = restTemplate.postForEntity(
                "/api/v1/funds/cancel", cancelRequest, Map.class);

        assertThat(cancelResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cancelResponse.getBody().get("type")).isEqualTo("CANCELACION");

        // Verificar que el balance se restituyó
        assertThat(capturedClient.get().balance()).isEqualByComparingTo(initialBalance);

        // === PASO 5: Consultar historial post-cancelación ===
        var txAfterCancel = capturedTransaction.get();
        when(transactionRepository.findByClientId(clientId, 0, 10))
                .thenReturn(new PageResult<>(List.of(txAfterSubscribe, txAfterCancel), 0, 10, 2, 1));

        ResponseEntity<Map> historyResponse2 = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}", Map.class, clientId);

        assertThat(historyResponse2.getStatusCode()).isEqualTo(HttpStatus.OK);
        var content2 = (List<Map<String, Object>>) historyResponse2.getBody().get("content");
        assertThat(content2).hasSize(2);

        // === PASO 6: Intentar cancelar de nuevo (debe fallar 404) ===
        ResponseEntity<Map> cancelAgainResponse = restTemplate.postForEntity(
                "/api/v1/funds/cancel", cancelRequest, Map.class);
        assertThat(cancelAgainResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        // Verificar notificaciones
        verify(notificationPort, times(2)).notify(any(Client.class), eq(fund), any(TransactionType.class));
    }
}
