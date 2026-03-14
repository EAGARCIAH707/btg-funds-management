package com.btg.funds.domain.exception;

public class SubscriptionNotFoundException extends DomainException {

    public SubscriptionNotFoundException(String clientId, String fundId) {
        super("El cliente " + clientId + " no está suscrito al fondo " + fundId);
    }
}
