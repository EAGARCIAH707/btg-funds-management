package com.btg.funds.adapter.in.rest;

import com.btg.funds.domain.port.in.CancelSubscriptionUseCase;
import com.btg.funds.domain.port.in.SubscribeToFundUseCase;
import com.btg.funds.adapter.in.rest.model.dto.SubscribeRequest;
import com.btg.funds.adapter.in.rest.model.dto.TransactionResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/funds")
public class FundController {

    private final SubscribeToFundUseCase subscribeToFundUseCase;
    private final CancelSubscriptionUseCase cancelSubscriptionUseCase;

    public FundController(SubscribeToFundUseCase subscribeToFundUseCase,
                          CancelSubscriptionUseCase cancelSubscriptionUseCase) {
        this.subscribeToFundUseCase = subscribeToFundUseCase;
        this.cancelSubscriptionUseCase = cancelSubscriptionUseCase;
    }

    @PostMapping("/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse subscribe(@Valid @RequestBody SubscribeRequest request) {
        var command = new SubscribeToFundUseCase.Command(request.clientId(), request.fundId());
        return TransactionResponse.from(subscribeToFundUseCase.execute(command));
    }

    @PostMapping("/cancel")
    public TransactionResponse cancel(@Valid @RequestBody SubscribeRequest request) {
        var command = new CancelSubscriptionUseCase.Command(request.clientId(), request.fundId());
        return TransactionResponse.from(cancelSubscriptionUseCase.execute(command));
    }
}
