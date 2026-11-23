package com.donsang.inventaire.repartition;

import java.util.List;

public record Repartition(List<LigneRepartition> lignes) {
    public Repartition {
        lignes = List.copyOf(lignes);
    }
}
