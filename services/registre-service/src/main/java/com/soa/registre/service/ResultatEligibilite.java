package com.soa.registre.service;

import java.time.LocalDate;

/**
 * Résultat de l'évaluation des règles d'éligibilité.
 *
 * @param eligible            true si toutes les règles sont respectées
 * @param motif               première règle non respectée (null si éligible)
 * @param prochainDonPossible date du dernier don + 56 jours, seulement si le délai
 *                            entre deux dons n'est pas respecté (sinon null)
 */
public record ResultatEligibilite(boolean eligible, String motif, LocalDate prochainDonPossible) {
}
