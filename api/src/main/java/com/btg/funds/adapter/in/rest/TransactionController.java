package com.btg.funds.adapter.in.rest;

import com.btg.funds.domain.port.in.GetTransactionHistoryUseCase;
import com.btg.funds.adapter.in.rest.model.dto.PaginatedResponse;
import com.btg.funds.adapter.in.rest.model.dto.TransactionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transactions", description = "Historial de transacciones")
public class TransactionController {

    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    public TransactionController(GetTransactionHistoryUseCase getTransactionHistoryUseCase) {
        this.getTransactionHistoryUseCase = getTransactionHistoryUseCase;
    }

    @GetMapping("/{clientId}")
    @Operation(summary = "Obtener historial de transacciones de un cliente")
    public PaginatedResponse<TransactionResponse> getHistory(
            @PathVariable String clientId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        var result = getTransactionHistoryUseCase.execute(clientId, page, size);
        return PaginatedResponse.from(result, TransactionResponse::from);
    }
}
