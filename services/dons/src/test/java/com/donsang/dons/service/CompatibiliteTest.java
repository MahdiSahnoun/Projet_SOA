package com.donsang.dons.service;

import static com.donsang.dons.domaine.GroupeSanguin.AB_POSITIF;
import static com.donsang.dons.domaine.GroupeSanguin.A_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.A_POSITIF;
import static com.donsang.dons.domaine.GroupeSanguin.O_NEGATIF;
import static com.donsang.dons.domaine.GroupeSanguin.O_POSITIF;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.donsang.dons.domaine.GroupeSanguin;

class CompatibiliteTest {

    @Test
    void oNegatifNeRecoitQueDuONegatif() {
        assertThat(Compatibilite.groupesCompatibles(O_NEGATIF)).containsExactly(O_NEGATIF);
    }

    @Test
    void aPositifRecoitDesGroupesAEtO() {
        assertThat(Compatibilite.groupesCompatibles(A_POSITIF))
                .containsExactlyInAnyOrder(A_POSITIF, A_NEGATIF, O_POSITIF, O_NEGATIF);
    }

    @Test
    void abPositifEstReceveurUniversel() {
        assertThat(Compatibilite.groupesCompatibles(AB_POSITIF))
                .containsExactlyInAnyOrder(GroupeSanguin.values());
    }

    @ParameterizedTest
    @EnumSource(GroupeSanguin.class)
    void oNegatifEstDonneurUniversel(GroupeSanguin receveur) {
        assertThat(Compatibilite.groupesCompatibles(receveur)).contains(O_NEGATIF);
    }

    @ParameterizedTest
    @EnumSource(GroupeSanguin.class)
    void leGroupeExactEstToujoursPropose(GroupeSanguin receveur) {
        assertThat(Compatibilite.groupesCompatibles(receveur).get(0)).isEqualTo(receveur);
    }
}
