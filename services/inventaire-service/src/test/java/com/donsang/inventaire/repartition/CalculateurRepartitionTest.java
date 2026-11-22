package com.donsang.inventaire.repartition;

import com.donsang.inventaire.service.StockInsuffisantException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CalculateurRepartitionTest {

    private final CalculateurRepartition calculateur = new CalculateurRepartition();

    @Test
    void stockSuffisantDansUneSeuleBanque() {
        Repartition repartition = calculateur.calculerRepartition(List.of(
                poche("A1", "BANQUE-A", 10),
                poche("A2", "BANQUE-A", 20),
                poche("A3", "BANQUE-A", 30)
        ), 2);

        assertThat(repartition.lignes()).containsExactly(
                new LigneRepartition("BANQUE-A", List.of("A1", "A2")));
    }

    @Test
    void repartitionPartielleEntrePlusieursBanques() {
        Repartition repartition = calculateur.calculerRepartition(List.of(
                poche("A1", "BANQUE-A", 10),
                poche("B1", "BANQUE-B", 11),
                poche("B2", "BANQUE-B", 12)
        ), 3);

        assertThat(repartition.lignes()).containsExactly(
                new LigneRepartition("BANQUE-A", List.of("A1")),
                new LigneRepartition("BANQUE-B", List.of("B1", "B2")));
    }

    @Test
    void stockNul() {
        assertThatThrownBy(() -> calculateur.calculerRepartition(List.of(), 1))
                .isInstanceOf(StockInsuffisantException.class)
                .hasMessageContaining("1");
    }

    @Test
    void stockInsuffisantSansRepartitionPartielle() {
        assertThatThrownBy(() -> calculateur.calculerRepartition(List.of(
                poche("A1", "BANQUE-A", 10),
                poche("B1", "BANQUE-B", 20)
        ), 4))
                .isInstanceOf(StockInsuffisantException.class)
                .hasMessageContaining("2");
    }

    @Test
    void quantiteZero() {
        assertThatThrownBy(() -> calculateur.calculerRepartition(List.of(
                poche("A1", "BANQUE-A", 10)
        ), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pochesTrieesParDateDePeremptionAvantRepartition() {
        Repartition repartition = calculateur.calculerRepartition(List.of(
                poche("B3", "BANQUE-B", 30),
                poche("A2", "BANQUE-A", 20),
                poche("C1", "BANQUE-C", 5),
                poche("A1", "BANQUE-A", 10)
        ), 3);

        assertThat(repartition.lignes()).containsExactly(
                new LigneRepartition("BANQUE-C", List.of("C1")),
                new LigneRepartition("BANQUE-A", List.of("A1", "A2")));
    }

    private static PocheDisponible poche(String id, String banqueId, int jours) {
        return new PocheDisponible(id, banqueId, LocalDate.of(2026, 1, 1).plusDays(jours));
    }
}
