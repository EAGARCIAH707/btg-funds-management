package com.btg.funds.adapter.in.rest;

import com.btg.funds.domain.exception.ClientNotFoundException;
import com.btg.funds.domain.model.PageResult;
import com.btg.funds.domain.model.Transaction;
import com.btg.funds.domain.model.enums.TransactionType;
import com.btg.funds.domain.port.in.GetTransactionHistoryUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    @Test
    void getHistory_shouldReturn200_withPaginatedResults() throws Exception {
        var transactions = List.of(
                new Transaction("tx-1", "client-001", "1", "FPV_BTG",
                        TransactionType.APERTURA, new BigDecimal("75000"), Instant.parse("2026-03-14T10:00:00Z"))
        );
        var pageResult = new PageResult<>(transactions, 0, 10, 1, 1);

        when(getTransactionHistoryUseCase.execute("client-001", 0, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/transactions/client-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$.content[0].type").value("APERTURA"))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void getHistory_shouldReturn200_withEmptyPage() throws Exception {
        var pageResult = new PageResult<Transaction>(List.of(), 0, 10, 0, 0);

        when(getTransactionHistoryUseCase.execute("client-001", 0, 10)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/transactions/client-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getHistory_shouldUsePaginationParams() throws Exception {
        var pageResult = new PageResult<Transaction>(List.of(), 2, 5, 15, 3);

        when(getTransactionHistoryUseCase.execute("client-001", 2, 5)).thenReturn(pageResult);

        mockMvc.perform(get("/api/v1/transactions/client-001")
                        .param("page", "2")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getHistory_shouldReturn404_whenClientNotFound() throws Exception {
        when(getTransactionHistoryUseCase.execute("unknown", 0, 10))
                .thenThrow(new ClientNotFoundException("unknown"));

        mockMvc.perform(get("/api/v1/transactions/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("NOT_FOUND"));
    }
}
