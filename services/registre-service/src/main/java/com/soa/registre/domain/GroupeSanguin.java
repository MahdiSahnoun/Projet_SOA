package com.soa.registre.domain;

/**
 * Groupe sanguin — mêmes noms que l'enum du service Dons et le contrat gRPC
 * (Dons envoie groupe.name() dans EnregistrerDon / ListerDonneursEligibles).
 */
public enum GroupeSanguin {
    O_NEGATIF, O_POSITIF,
    A_NEGATIF, A_POSITIF,
    B_NEGATIF, B_POSITIF,
    AB_NEGATIF, AB_POSITIF
}
