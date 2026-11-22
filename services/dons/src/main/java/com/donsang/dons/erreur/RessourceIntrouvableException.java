package com.donsang.dons.erreur;

/** Traduite en 404 Not Found. */
public class RessourceIntrouvableException extends RuntimeException {

    public RessourceIntrouvableException(String message) {
        super(message);
    }
}
