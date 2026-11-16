package com.donsang.dons.erreur;

public class InventaireIndisponibleException extends ServiceIndisponibleException {

    public InventaireIndisponibleException(Throwable cause) {
        super("Service Inventaire indisponible", cause);
    }
}
