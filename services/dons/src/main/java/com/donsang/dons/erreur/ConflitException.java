package com.donsang.dons.erreur;

/** Traduite en 409 Conflict : la requête est valide mais contredit l'état actuel. */
public class ConflitException extends RuntimeException {

    public ConflitException(String message) {
        super(message);
    }
}
