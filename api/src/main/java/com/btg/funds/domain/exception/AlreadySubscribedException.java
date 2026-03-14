package com.btg.funds.domain.exception;

public class AlreadySubscribedException extends DomainException {

    public AlreadySubscribedException(String fundName) {
        super("Ya está suscrito al fondo " + fundName);
    }
}
