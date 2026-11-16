package com.donsang.dons.erreur;

/** Réponse métier d'Inventaire (RESOURCE_EXHAUSTED) : ce n'est pas une panne. */
public class StockInsuffisantException extends RuntimeException {

    public StockInsuffisantException(String message) {
        super(message);
    }
}
