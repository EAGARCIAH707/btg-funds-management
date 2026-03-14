package com.btg.funds.adapter.in.rest.model.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscribeRequest(
        @NotBlank String clientId,
        @NotBlank String fundId
) {
}
