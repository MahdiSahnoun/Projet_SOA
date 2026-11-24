package com.donsang.inventaire.grpc;

import com.donsang.inventaire.domain.Banque;
import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.repository.BanqueRepository;
import com.donsang.inventaire.repository.PocheSangRepository;
import com.donsang.inventaire.service.InventaireService;
import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.domain.StatutPoche;
import com.donsang.inventaire.service.StockInsuffisantException;
import com.donsang.inventaire.v1.AjoutPocheRequest;
import com.donsang.inventaire.v1.InventaireServiceGrpc;
import com.donsang.inventaire.v1.PocheReponse;
import com.donsang.inventaire.v1.ReferenceRequest;
import com.donsang.inventaire.v1.ReservationRequest;
import com.donsang.inventaire.v1.ReservationReponse;
import com.donsang.inventaire.v1.StockRequest;
import com.donsang.inventaire.v1.StockReponse;
import com.donsang.inventaire.v1.Ack;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=never",
        "grpc.server.port=-1"
})
@Testcontainers(disabledWithoutDocker = true)
class InventaireGrpcIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    private static Server serveur;
    private static ManagedChannel canal;

    @DynamicPropertySource
    static void proprietes(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private BanqueRepository banques;

    @Autowired
    private PocheSangRepository poches;

    @Autowired
    private InventaireService inventaire;

    @Autowired
    private InventaireGrpcService grpcService;

    private InventaireServiceGrpc.InventaireServiceBlockingStub stub;

    @BeforeEach
    void preparer() throws Exception {
        poches.deleteAll();
        enregistrerBanque("BANQUE-A", "Banque A", "Sfax");
        enregistrerBanque("BANQUE-B", "Banque B", "Tunis");
        enregistrerBanque("BANQUE-C", "Banque C", "Sousse");

        if (serveur == null) {
            String nom = "inventaire-test-" + UUID.randomUUID();
            serveur = InProcessServerBuilder.forName(nom)
                    .directExecutor()
                    .addService(grpcService)
                    .build()
                    .start();
            canal = InProcessChannelBuilder.forName(nom)
                    .directExecutor()
                    .build();
        }
        stub = InventaireServiceGrpc.newBlockingStub(canal);
    }

    private void enregistrerBanque(String id, String nom, String ville) {
        if (banques.findById(id).isEmpty()) {
            banques.save(new Banque(id, nom, ville));
        }
    }

    @AfterAll
    static void arreterServeur() {
        if (canal != null) {
            canal.shutdownNow();
        }
        if (serveur != null) {
            serveur.shutdownNow();
        }
    }

    @Test
    void ajouterPocheValide() {
        PocheReponse reponse = stub.ajouterPoche(AjoutPocheRequest.newBuilder()
                .setBanqueId("BANQUE-A")
                .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .setVolumeMl(450)
                .setDateCollecte(LocalDate.now().minusDays(1).toString())
                .setDatePeremption(LocalDate.now().plusDays(42).toString())
                .build());

        assertThat(reponse.getPocheId()).startsWith("POCHE-");
    }

    @Test
    void ajouterPocheBanqueInconnue() {
        StatusRuntimeException erreur = assertThrows(StatusRuntimeException.class,
                () -> stub.ajouterPoche(AjoutPocheRequest.newBuilder()
                        .setBanqueId("BANQUE-X")
                        .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                        .setVolumeMl(450)
                        .setDateCollecte(LocalDate.now().minusDays(1).toString())
                        .setDatePeremption(LocalDate.now().plusDays(42).toString())
                        .build()));

        assertThat(erreur.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void ajouterPocheDatesInvalides() {
        StatusRuntimeException dateFuture = assertThrows(StatusRuntimeException.class,
                () -> stub.ajouterPoche(AjoutPocheRequest.newBuilder()
                        .setBanqueId("BANQUE-A")
                        .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.A_POSITIF)
                        .setVolumeMl(450)
                        .setDateCollecte(LocalDate.now().plusDays(1).toString())
                        .setDatePeremption(LocalDate.now().plusDays(42).toString())
                        .build()));
        assertThat(dateFuture.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);

        StatusRuntimeException datesEgales = assertThrows(StatusRuntimeException.class,
                () -> stub.ajouterPoche(AjoutPocheRequest.newBuilder()
                        .setBanqueId("BANQUE-A")
                        .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.A_POSITIF)
                        .setVolumeMl(450)
                        .setDateCollecte(LocalDate.now().toString())
                        .setDatePeremption(LocalDate.now().toString())
                        .build()));
        assertThat(datesEgales.getStatus().getCode()).isEqualTo(Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void ajouterPocheDoublonAuNiveauMetier() {
        inventaire.ajouterPoche("POCHE-DOUBLON", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(42));

        var erreur = assertThrows(
                com.donsang.inventaire.service.exceptions.PocheExistanteException.class,
                () -> inventaire.ajouterPoche("POCHE-DOUBLON", "BANQUE-A",
                        GroupeSanguin.O_NEGATIF, 450,
                        LocalDate.now().minusDays(1), LocalDate.now().plusDays(42)));
        assertThat(erreur).hasMessageContaining("POCHE-DOUBLON");
    }

    @Test
    void confirmerSortieValide() {
        inventaire.ajouterPoche("POCHE-TEST-SORTIE", "BANQUE-B",
                GroupeSanguin.A_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(42));
        inventaire.reserver(List.of(GroupeSanguin.A_NEGATIF),
                1, "DEM-TEST-SORTIE");

        var reponse = stub.confirmerSortie(ReferenceRequest.newBuilder()
                .setReferenceDemande("DEM-TEST-SORTIE")
                .build());

        assertThat(reponse.getOk()).isTrue();
    }

    @Test
    void confirmerSortieReferenceInconnue() {
        StatusRuntimeException erreur = assertThrows(StatusRuntimeException.class,
                () -> stub.confirmerSortie(ReferenceRequest.newBuilder()
                        .setReferenceDemande("DEM-INCONNUE")
                        .build()));

        assertThat(erreur.getStatus().getCode()).isEqualTo(Status.Code.NOT_FOUND);
    }

    @Test
    void reservationRepartieSurDeuxBanquesEtIdempotente() {
        inventaire.ajouterPoche("POCHE-RA1", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));
        inventaire.ajouterPoche("POCHE-RB1", "BANQUE-B",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(20));
        inventaire.ajouterPoche("POCHE-RB2", "BANQUE-B",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        var reponse = stub.reserverPoches(ReservationRequest.newBuilder()
                .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .setQuantite(3)
                .setReferenceDemande("DEM-REPARTITION")
                .build());

        assertThat(reponse.getAllocationsCount()).isEqualTo(2);
        assertThat(reponse.getAllocationsList())
                .extracting(com.donsang.inventaire.v1.AllocationBanque::getBanqueId)
                .containsExactly("BANQUE-A", "BANQUE-B");

        var idem = stub.reserverPoches(ReservationRequest.newBuilder()
                .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .setQuantite(3)
                .setReferenceDemande("DEM-REPARTITION")
                .build());
        assertThat(idem.getAllocationsList())
                .isEqualTo(reponse.getAllocationsList());
    }

    @Test
    void reservationStockInsuffisantSansRepartitionPartielle() {
        inventaire.ajouterPoche("POCHE-SI1", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(20));

        StatusRuntimeException erreur = assertThrows(StatusRuntimeException.class,
                () -> stub.reserverPoches(ReservationRequest.newBuilder()
                        .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                        .setQuantite(2)
                        .setReferenceDemande("DEM-STOCK-INSUFFISANT")
                        .build()));

        assertThat(erreur.getStatus().getCode()).isEqualTo(Status.Code.RESOURCE_EXHAUSTED);
        PocheSang poche = poches.findById("POCHE-SI1").orElseThrow();
        assertThat(poche.getStatut()).isEqualTo(StatutPoche.DISPONIBLE);
        assertThat(poche.getReferenceReservation()).isNull();
    }

    @Test
    void reservationNePrendJamaisUnePochePerimee() {
        PocheSang perimee = poches.save(new PocheSang(
                "POCHE-PERIMEE",
                banques.findById("BANQUE-C").orElseThrow(),
                GroupeSanguin.O_NEGATIF,
                LocalDate.now().minusDays(10),
                LocalDate.now()));
        perimee.setStatut(StatutPoche.PERIMEE);
        poches.save(perimee);

        inventaire.ajouterPoche("POCHE-VALIDE", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(20));

        var reponse = stub.reserverPoches(ReservationRequest.newBuilder()
                .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .setQuantite(1)
                .setReferenceDemande("DEM-SANS-PERIMEE")
                .build());

        assertThat(reponse.getAllocationsCount()).isEqualTo(1);
        assertThat(reponse.getAllocations(0).getBanqueId()).isEqualTo("BANQUE-A");
        assertThat(poches.findById("POCHE-PERIMEE").orElseThrow().getStatut())
                .isEqualTo(StatutPoche.PERIMEE);
    }

    @Test
    void parcoursGrpcT23ReservationLiberationEtStock() {
        inventaire.ajouterPoche("POCHE-T23-A", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(10));
        inventaire.ajouterPoche("POCHE-T23-B1", "BANQUE-B",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(20));
        inventaire.ajouterPoche("POCHE-T23-B2", "BANQUE-B",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        StockReponse stockInitial = stub.getStock(StockRequest.newBuilder()
                .setBanqueId("")
                .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .build());
        assertThat(stockInitial.getDisponibles()).isEqualTo(3);

        ReservationReponse reservation = stub.reserverPoches(
                ReservationRequest.newBuilder()
                        .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                        .setQuantite(3)
                        .setReferenceDemande("DEM-T23")
                        .build());
        assertThat(reservation.getAllocationsCount()).isEqualTo(2);
        assertThat(reservation.getAllocationsList())
                .extracting(com.donsang.inventaire.v1.AllocationBanque::getBanqueId)
                .containsExactly("BANQUE-A", "BANQUE-B");

        StockReponse stockApresReservation = stub.getStock(StockRequest.newBuilder()
                .setBanqueId("")
                .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .build());
        assertThat(stockApresReservation.getDisponibles()).isZero();

        StatusRuntimeException stockInsuffisant = assertThrows(StatusRuntimeException.class,
                () -> stub.reserverPoches(ReservationRequest.newBuilder()
                        .addGroupes(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                        .setQuantite(1)
                        .setReferenceDemande("DEM-T23-INSUFFISANT")
                        .build()));
        assertThat(stockInsuffisant.getStatus().getCode())
                .isEqualTo(Status.Code.RESOURCE_EXHAUSTED);

        Ack liberation = stub.libererReservation(ReferenceRequest.newBuilder()
                .setReferenceDemande("DEM-T23")
                .build());
        assertThat(liberation.getOk()).isTrue();

        StockReponse stockApresLiberation = stub.getStock(StockRequest.newBuilder()
                .setBanqueId("")
                .setGroupe(com.donsang.inventaire.v1.GroupeSanguin.O_NEGATIF)
                .build());
        assertThat(stockApresLiberation.getDisponibles()).isEqualTo(3);
    }

    @Test
    void reservationsConcurrentesNePrennentPasLaMemePoche() throws Exception {
        inventaire.ajouterPoche("POCHE-CONC-A", "BANQUE-A",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(20));
        inventaire.ajouterPoche("POCHE-CONC-B", "BANQUE-B",
                GroupeSanguin.O_NEGATIF, 450,
                LocalDate.now().minusDays(1), LocalDate.now().plusDays(30));

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch depart = new CountDownLatch(1);

        Future<List<InventaireService.Allocation>> premiere = executor.submit(() -> {
            depart.await();
            return inventaire.reserverPoches("DEM-CONC-A",
                    List.of(GroupeSanguin.O_NEGATIF), 1);
        });
        Future<List<InventaireService.Allocation>> seconde = executor.submit(() -> {
            depart.await();
            return inventaire.reserverPoches("DEM-CONC-B",
                    List.of(GroupeSanguin.O_NEGATIF), 1);
        });

        depart.countDown();
        try {
            // Les deux réservations doivent réussir : chacune verrouille une seule poche
            assertThat(premiere.get()).isNotEmpty();
            assertThat(seconde.get()).isNotEmpty();
        } finally {
            executor.shutdownNow();
        }

        // Lecture APRÈS la fin des deux transactions
        PocheSang concA = poches.findById("POCHE-CONC-A").orElseThrow();
        PocheSang concB = poches.findById("POCHE-CONC-B").orElseThrow();
        assertThat(concA.getStatut()).isEqualTo(StatutPoche.RESERVEE);
        assertThat(concB.getStatut()).isEqualTo(StatutPoche.RESERVEE);
        assertThat(List.of(concA.getReferenceReservation(), concB.getReferenceReservation()))
                .containsExactlyInAnyOrder("DEM-CONC-A", "DEM-CONC-B");
    }
}
