package com.donsang.inventaire.repartition;

import java.util.List;

public record LigneRepartition(String banqueId, List<String> pocheIds) {
    public LigneRepartition {
        pocheIds = List.copyOf(pocheIds);
    }
}
