package com.donsang.inventaire.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * T42 — Tests unitaires de la règle de péremption.
 * Règle : une poche est périmée dès sa date de péremption (le jour même compris).
 * La date du jour est toujours fixée par le test : aucun test ne dépend de la date réelle.
 */
class RegleDePeremptionTest {

    private static final LocalDate DATE_PEREMPTION = LocalDate.of(2026, 3, 15);

    /** Horloge figée au jour de péremption, pour la version qui lit la date du jour. */
    private final Clock horlogeJourMeme = Clock.fixed(
            DATE_PEREMPTION.atStartOfDay(ZoneId.of("Africa/Tunis")).toInstant(), ZoneId.of("Africa/Tunis"));

    // Le dépôt et l'éditeur d'événements ne servent qu'à appliquerPeremption (T41), pas à estPerimee.
    private final RegleDePeremption regle = new RegleDePeremption(null, null, horlogeJourMeme);

    @Test
    void laVeilleDeLaDateLimite_laPocheNEstPasPerimee() {
        assertThat(regle.estPerimee(DATE_PEREMPTION, DATE_PEREMPTION.minusDays(1))).isFalse();
    }

    @Test
    void leJourMeme_laPocheEstPerimee() {
        assertThat(regle.estPerimee(DATE_PEREMPTION, DATE_PEREMPTION)).isTrue();
    }

    @Test
    void leLendemain_laPocheEstPerimee() {
        assertThat(regle.estPerimee(DATE_PEREMPTION, DATE_PEREMPTION.plusDays(1))).isTrue();
    }

    @Test
    void sansDateDePeremption_laPocheNEstPasPerimee() {
        assertThat(regle.estPerimee(null, DATE_PEREMPTION)).isFalse();
    }

    @Test
    void laVersionSansDateUtiliseLHorlogeInjectee() {
        // Horloge figée au 15/03/2026 : périmée le 15, pas encore le 16.
        assertThat(regle.estPerimee(DATE_PEREMPTION)).isTrue();
        assertThat(regle.estPerimee(DATE_PEREMPTION.plusDays(1))).isFalse();
    }
}
