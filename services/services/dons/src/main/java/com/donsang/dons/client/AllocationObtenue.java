package com.donsang.dons.client;

import com.donsang.dons.domaine.GroupeSanguin;

/** Une ligne de la répartition renvoyée par Inventaire, déjà convertie en types du domaine. */
public record AllocationObtenue(String codeBanque, GroupeSanguin groupeSanguin, int quantite) {
}
