package com.btg.funds.domain.exception;

public class ClientNotFoundException extends DomainException {

    public ClientNotFoundException(String clientId) {
        super("Cliente no encontrado: " + clientId);
    }
}
