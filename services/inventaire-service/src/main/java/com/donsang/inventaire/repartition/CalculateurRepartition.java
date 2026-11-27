package com.donsang.inventaire.repartition;

import com.donsang.inventaire.service.StockInsuffisantException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CalculateurRepartition {

    public Repartition calculerRepartition(List<PocheDisponible> poches, int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantité doit être strictement positive");
        }

        List<PocheDisponible> disponibles = poches == null ? List.of() : poches;
        if (disponibles.size() < quantite) {
            int nombreManquant = quantite - disponibles.size();
            throw new StockInsuffisantException(
                    "Stock insuffisant: " + nombreManquant + " poche(s) manquante(s)");
        }

        List<PocheDisponible> selectionnees = disponibles.stream()
                .sorted(Comparator.comparing(PocheDisponible::datePeremption))
                .limit(quantite)
                .toList();

        Map<String, List<String>> idsParBanque = new LinkedHashMap<>();
        for (PocheDisponible poche : selectionnees) {
            idsParBanque.computeIfAbsent(poche.banqueId(), banque -> new ArrayList<>())
                    .add(poche.id());
        }

        List<LigneRepartition> lignes = idsParBanque.entrySet().stream()
                .map(entry -> new LigneRepartition(entry.getKey(), entry.getValue()))
                .toList();
        return new Repartition(lignes);
    }
}
