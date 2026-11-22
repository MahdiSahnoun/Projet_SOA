package com.donsang.inventaire.service.exceptions;

public class BanqueInconnueException extends RuntimeException {
    public BanqueInconnueException(String message) {
        super(message);
    }
}
