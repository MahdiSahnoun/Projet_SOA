package com.donsang.inventaire.service;

import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.repository.BanqueRepository;
import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.repository.PocheSangRepository;
import com.donsang.inventaire.service.exceptions.BanqueInconnueException;
import com.donsang.inventaire.service.exceptions.AucunePocheReserveeException;
import com.donsang.inventaire.service.exceptions.PocheExistanteException;
import com.donsang.inventaire.service.exceptions.PochePerimeeException;
import com.donsang.inventaire.service.exceptions.ReferenceInconnueException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.datasource.url=jdbc:h2:mem:inventaire;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "grpc.server.port=-1"
})
@ActiveProfiles("test")
class InventaireServiceTest {

    @Autowired
    private InventaireService inventaire;

    @Autowired
    private PocheSangRepository poches;

    @Autowired
    private BanqueRepository banques;

    @BeforeEach
    void viderPoches() {
        poches.deleteAll();
    }

    @Test
    void reserveToutOuRien() {
        inventaire.ajouterPoche("BANQUE-A", GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now(), LocalDate.now().plusDays(42));
        inventaire.ajouterPoche("BANQUE-B", GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now(), LocalDate.now().plusDays(42));

        assertThatThrownBy(() -> inventaire.reserver(
                List.of(GroupeSanguin.O_NEGATIF), 3, "DEM-1"))
                .isInstanceOf(StockInsuffisantException.class);

        assertThat(inventaire.getStock(GroupeSanguin.O_NEGATIF, null)).isEqualTo(2);
    }

    @Test
    void reservePuisLibereSurLaReference() {
        PocheSang poche = inventaire.ajouterPoche(
                "BANQUE-C", GroupeSanguin.A_NEGATIF, 450,
                LocalDate.now(), LocalDate.now().plusDays(42));

        List<InventaireService.Allocation> allocations =
                inventaire.reserver(List.of(GroupeSanguin.A_NEGATIF, GroupeSanguin.O_NEGATIF),
                        1, "DEM-42");

        assertThat(allocations).containsExactly(
                new InventaireService.Allocation("BANQUE-C", GroupeSanguin.A_NEGATIF, 1));
        assertThat(inventaire.getStock(GroupeSanguin.A_NEGATIF, "BANQUE-C")).isZero();

        inventaire.liberer("DEM-42");
        assertThat(inventaire.getStock(GroupeSanguin.A_NEGATIF, "BANQUE-C")).isEqualTo(1);
        assertThat(poche.getReferenceReservation()).isNull();
    }

    @Test
    void refuseUneBanqueInconnue() {
        assertThatThrownBy(() -> inventaire.ajouterPoche(
                "BANQUE-X", GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now(), LocalDate.now().plusDays(42)))
                .isInstanceOf(BanqueInconnueException.class);
    }

    @Test
    void ajoutePocheEtDetecteUnIdentifiantExistant() {
        inventaire.ajouterPoche("POCHE-ID-UNIQUE", "BANQUE-A", GroupeSanguin.A_POSITIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(42));

        assertThatThrownBy(() -> inventaire.ajouterPoche(
                "POCHE-ID-UNIQUE", "BANQUE-A", GroupeSanguin.A_POSITIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(42)))
                .isInstanceOf(PocheExistanteException.class);
    }

    @Test
    void refuseUnePocheExpireeAuJourDeSonAjout() {
        assertThatThrownBy(() -> inventaire.ajouterPoche(
                "POCHE-EXPIREE", "BANQUE-A", GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(10), LocalDate.now()))
                .isInstanceOf(PochePerimeeException.class);
    }

    @Test
    void confirmerSortieVerifieLaReference() {
        assertThatThrownBy(() -> inventaire.confirmerSortie("DEM-INCONNUE"))
                .isInstanceOf(ReferenceInconnueException.class);

        PocheSang poche = inventaire.ajouterPoche(
                "POCHE-SANS-RESERVE", "BANQUE-C", GroupeSanguin.B_POSITIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(42));
        assertThat(poche.getStatut()).isEqualTo(com.donsang.inventaire.domain.StatutPoche.DISPONIBLE);

        assertThatThrownBy(() -> inventaire.confirmerSortie("DEM-SANS-RESERVE"))
                .isInstanceOf(ReferenceInconnueException.class);
    }
}
