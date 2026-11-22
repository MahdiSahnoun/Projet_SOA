package com.donsang.inventaire.service.exceptions;

public class ReferenceInconnueException extends RuntimeException {
    public ReferenceInconnueException(String message) {
        super(message);
    }
}
