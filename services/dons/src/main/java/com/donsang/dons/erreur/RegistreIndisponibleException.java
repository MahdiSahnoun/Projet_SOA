package com.donsang.dons.erreur;

public class RegistreIndisponibleException extends ServiceIndisponibleException {

    public RegistreIndisponibleException(Throwable cause) {
        super("Registre national indisponible", cause);
    }
}
