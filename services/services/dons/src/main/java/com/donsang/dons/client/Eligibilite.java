package com.donsang.dons.client;

/** Résultat de la vérification auprès du Registre. */
public record Eligibilite(Etat etat, String motif) {

    public enum Etat {
        ELIGIBLE,
        INELIGIBLE,
        /** Registre injoignable : la vérification est différée (méthode de repli). */
        EN_ATTENTE
    }

    public static Eligibilite eligible() {
        return new Eligibilite(Etat.ELIGIBLE, null);
    }

    public static Eligibilite refusee(String motif) {
        return new Eligibilite(Etat.INELIGIBLE, motif);
    }

    public static Eligibilite enAttente(String motif) {
        return new Eligibilite(Etat.EN_ATTENTE, motif);
    }
}
