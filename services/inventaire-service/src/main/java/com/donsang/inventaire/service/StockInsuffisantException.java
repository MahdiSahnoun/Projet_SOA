package com.donsang.inventaire.service;

public class StockInsuffisantException extends RuntimeException {
    public StockInsuffisantException(String message) {
        super(message);
    }
}
