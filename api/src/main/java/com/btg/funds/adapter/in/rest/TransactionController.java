package com.btg.funds.adapter.in.rest;

import com.btg.funds.domain.port.in.GetTransactionHistoryUseCase;
import com.btg.funds.adapter.in.rest.model.dto.TransactionResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final GetTransactionHistoryUseCase getTransactionHistoryUseCase;

    public TransactionController(GetTransactionHistoryUseCase getTransactionHistoryUseCase) {
        this.getTransactionHistoryUseCase = getTransactionHistoryUseCase;
    }

    @GetMapping("/{clientId}")
    public List<TransactionResponse> getHistory(@PathVariable String clientId) {
        return getTransactionHistoryUseCase.execute(clientId).stream()
                .map(TransactionResponse::from)
                .toList();
    }
}
