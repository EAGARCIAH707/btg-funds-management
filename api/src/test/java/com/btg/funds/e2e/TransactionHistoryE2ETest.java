package com.btg.funds.e2e;

import com.btg.funds.domain.model.Client;
import com.btg.funds.domain.model.PageResult;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionHistoryE2ETest {

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

    private Client client;

    @BeforeEach
    void setUp() {
        client = new Client(CLIENT_ID, "Juan Pérez", "juan@email.com", "+573001234567",
                new BigDecimal("500000"), NotificationPreference.EMAIL);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{clientId} - historial con resultados retorna 200")
    void getHistorySuccess() {
        var tx1 = new Transaction("tx-001", CLIENT_ID, "fund-001", "FPV_RECAUDADORA",
                TransactionType.APERTURA, new BigDecimal("75000"), Instant.parse("2025-01-15T10:00:00Z"));
        var tx2 = new Transaction("tx-002", CLIENT_ID, "fund-002", "FPV_ECOPETROL",
                TransactionType.CANCELACION, new BigDecimal("50000"), Instant.parse("2025-01-16T14:30:00Z"));

        var pageResult = new PageResult<>(List.of(tx1, tx2), 0, 10, 2, 1);

        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId(CLIENT_ID, 0, 10)).thenReturn(pageResult);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}?page=0&size=10", Map.class, CLIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("page")).isEqualTo(0);
        assertThat(response.getBody().get("size")).isEqualTo(10);
        assertThat(response.getBody().get("totalElements")).isEqualTo(2);
        assertThat(response.getBody().get("totalPages")).isEqualTo(1);

        var content = (List<Map<String, Object>>) response.getBody().get("content");
        assertThat(content).hasSize(2);
        assertThat(content.get(0).get("transactionId")).isEqualTo("tx-001");
        assertThat(content.get(0).get("type")).isEqualTo("APERTURA");
        assertThat(content.get(1).get("transactionId")).isEqualTo("tx-002");
        assertThat(content.get(1).get("type")).isEqualTo("CANCELACION");
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{clientId} - historial vacío retorna 200")
    void getHistoryEmpty() {
        var emptyPage = new PageResult<Transaction>(List.of(), 0, 10, 0, 0);

        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId(CLIENT_ID, 0, 10)).thenReturn(emptyPage);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}", Map.class, CLIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat((List<?>) response.getBody().get("content")).isEmpty();
        assertThat(response.getBody().get("totalElements")).isEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{clientId} - cliente no encontrado retorna 404")
    void getHistoryClientNotFound() {
        when(clientRepository.findById("unknown")).thenReturn(Optional.empty());

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}", Map.class, "unknown");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().get("error")).isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("GET /api/v1/transactions/{clientId} - paginación custom page=1 size=5")
    void getHistoryCustomPagination() {
        var tx = new Transaction("tx-006", CLIENT_ID, "fund-003", "DEUDA_PRIVADA",
                TransactionType.APERTURA, new BigDecimal("100000"), Instant.parse("2025-02-01T08:00:00Z"));
        var pageResult = new PageResult<>(List.of(tx), 1, 5, 8, 2);

        when(clientRepository.findById(CLIENT_ID)).thenReturn(Optional.of(client));
        when(transactionRepository.findByClientId(CLIENT_ID, 1, 5)).thenReturn(pageResult);

        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/transactions/{clientId}?page=1&size=5", Map.class, CLIENT_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("page")).isEqualTo(1);
        assertThat(response.getBody().get("size")).isEqualTo(5);
        assertThat(response.getBody().get("totalElements")).isEqualTo(8);
        assertThat(response.getBody().get("totalPages")).isEqualTo(2);
        assertThat((List<?>) response.getBody().get("content")).hasSize(1);
    }
}
