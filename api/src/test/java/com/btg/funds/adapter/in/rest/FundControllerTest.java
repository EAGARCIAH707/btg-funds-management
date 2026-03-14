package com.btg.funds.adapter.in.rest;

import com.btg.funds.domain.exception.AlreadySubscribedException;
import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.exception.FundNotFoundException;
import com.btg.funds.domain.exception.InsufficientBalanceException;
import com.btg.funds.domain.exception.SubscriptionNotFoundException;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.CancelSubscriptionUseCase;
import com.btg.funds.domain.port.in.SubscribeToFundUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FundController.class)
class FundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SubscribeToFundUseCase subscribeToFundUseCase;

    @MockitoBean
    private CancelSubscriptionUseCase cancelSubscriptionUseCase;

    private static final Transaction TRANSACTION = new Transaction(
            "tx-001", "client-001", "1", "FPV_BTG", TransactionType.APERTURA,
            new BigDecimal("75000"), Instant.parse("2026-03-14T10:00:00Z")
    );

    @Test
    void subscribe_shouldReturn201_whenSuccessful() throws Exception {
        when(subscribeToFundUseCase.execute(any())).thenReturn(TRANSACTION);

        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "1"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.transactionId").value("tx-001"))
                .andExpect(jsonPath("$.fundId").value("1"))
                .andExpect(jsonPath("$.type").value("APERTURA"))
                .andExpect(jsonPath("$.amount").value(75000));
    }

    @Test
    void subscribe_shouldReturn400_whenClientIdBlank() throws Exception {
        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "", "fundId": "1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test
    void subscribe_shouldReturn400_whenBodyMissing() throws Exception {
        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void subscribe_shouldReturn404_whenClientNotFound() throws Exception {
        when(subscribeToFundUseCase.execute(any()))
                .thenThrow(new ClientNotFoundException("client-999"));

        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-999", "fundId": "1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void subscribe_shouldReturn404_whenFundNotFound() throws Exception {
        when(subscribeToFundUseCase.execute(any()))
                .thenThrow(new FundNotFoundException("99"));

        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "99"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }

    @Test
    void subscribe_shouldReturn409_whenAlreadySubscribed() throws Exception {
        when(subscribeToFundUseCase.execute(any()))
                .thenThrow(new AlreadySubscribedException("FPV_BTG"));

        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "1"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ALREADY_SUBSCRIBED"));
    }

    @Test
    void subscribe_shouldReturn400_whenInsufficientBalance() throws Exception {
        when(subscribeToFundUseCase.execute(any()))
                .thenThrow(new InsufficientBalanceException("FPV_BTG"));

        mockMvc.perform(post("/api/v1/funds/subscribe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "1"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INSUFFICIENT_BALANCE"));
    }

    @Test
    void cancel_shouldReturn200_whenSuccessful() throws Exception {
        var cancelTx = new Transaction(
                "tx-002", "client-001", "1", "FPV_BTG", TransactionType.CANCELACION,
                new BigDecimal("75000"), Instant.parse("2026-03-14T10:00:00Z")
        );
        when(cancelSubscriptionUseCase.execute(any())).thenReturn(cancelTx);

        mockMvc.perform(post("/api/v1/funds/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "1"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CANCELACION"));
    }

    @Test
    void cancel_shouldReturn404_whenSubscriptionNotFound() throws Exception {
        when(cancelSubscriptionUseCase.execute(any()))
                .thenThrow(new SubscriptionNotFoundException("client-001", "1"));

        mockMvc.perform(post("/api/v1/funds/cancel")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"clientId": "client-001", "fundId": "1"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
