package com.donsang.dons.domaine;

/**
 * Statut commun aux dons et aux demandes (diagramme de classes).
 * ANNULEE est ajouté pour l'annulation d'une demande (US6).
 */
public enum StatutServiceDon {
    EN_ATTENTE,
    ACCEPTEE,
    REFUSEE,
    ANNULEE
}
