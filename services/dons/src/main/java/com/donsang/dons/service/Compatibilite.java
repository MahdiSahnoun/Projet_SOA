package com.donsang.dons.service;

import static com.donsang.dons.domaine.GroupeSanguin.AB_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.AB_POSITIF;
import static com.donsang.dons.domaine.GroupeSanguin.A_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.A_POSITIF;
import static com.donsang.dons.domaine.GroupeSanguin.B_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.B_POSITIF;
import static com.donsang.dons.domaine.GroupeSanguin.O_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.O_POSITIF;

import java.util.List;
import java.util.Map;

import com.donsang.dons.domaine.GroupeSanguin;

/**
 * Compatibilité des globules rouges (ABO / Rhésus) : pour un receveur,
 * quels groupes de donneurs conviennent. Le groupe exact vient en premier.
 * La règle reste dans Dons : Inventaire ne connaît que les groupes qu'on lui demande.
 */
public final class Compatibilite {

    private static final Map<GroupeSanguin, List<GroupeSanguin>> DONNEURS_COMPATIBLES = Map.of(
            O_NEGATIF, List.of(O_NEGATIF),
            O_POSITIF, List.of(O_POSITIF, O_NEGATIF),
            A_NEGATIF, List.of(A_NEGATIF, O_NEGATIF),
            A_POSITIF, List.of(A_POSITIF, A_NEGATIF, O_POSITIF, O_NEGATIF),
            B_NEGATIF, List.of(B_NEGATIF, O_NEGATIF),
            B_POSITIF, List.of(B_POSITIF, B_NEGATIF, O_POSITIF, O_NEGATIF),
            AB_NEGATIF, List.of(AB_NEGATIF, A_NEGATIF, B_NEGATIF, O_NEGATIF),
            AB_POSITIF, List.of(AB_POSITIF, AB_NEGATIF, A_POSITIF, A_NEGATIF,
                    B_POSITIF, B_NEGATIF, O_POSITIF, O_NEGATIF));

    private Compatibilite() {
    }

    public static List<GroupeSanguin> groupesCompatibles(GroupeSanguin receveur) {
        return DONNEURS_COMPATIBLES.get(receveur);
    }
}
