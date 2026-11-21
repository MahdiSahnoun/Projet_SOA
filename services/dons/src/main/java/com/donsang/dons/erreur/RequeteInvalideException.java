package com.donsang.dons.erreur;

/** Règle métier non respectée par les données envoyées ; traduite en 400. */
public class RequeteInvalideException extends RuntimeException {

    public RequeteInvalideException(String message) {
        super(message);
    }
}
