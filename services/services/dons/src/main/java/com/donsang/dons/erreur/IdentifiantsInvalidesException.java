package com.donsang.dons.erreur;

/** Traduite en 401, avec un message volontairement neutre (on ne dit pas si l'email existe). */
public class IdentifiantsInvalidesException extends RuntimeException {

    public IdentifiantsInvalidesException() {
        super("Identifiants invalides");
    }
}
