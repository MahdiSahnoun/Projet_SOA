package com.donsang.dons.api;

import static com.donsang.dons.domaine.GroupeSanguin.O_NEGATIF;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.donsang.dons.Fixtures;
import com.donsang.dons.client.AllocationObtenue;
import com.donsang.dons.client.InventaireClient;
import com.donsang.dons.client.RegistreClient;
import com.donsang.dons.depot.DonneurRepository;
import com.donsang.dons.depot.HopitalRepository;
import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.erreur.StockInsuffisantException;
import com.donsang.dons.notification.EmailNotificationPublisher;
import com.donsang.dons.notification.EmailNotificationEvent;
import com.donsang.dons.securite.JwtService;
import com.jayway.jsonpath.JsonPath;

/** Test d'intégration REST : vraie chaîne HTTP + sécurité + JPA (H2) ; Inventaire est simulé. */
// Pas de Config Server ni d'Eureka pendant les tests (sinon dons.yml remplace la base H2 du profil test)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DemandeApiIT {

    private static final String DEMANDE = """
            {"groupeSanguin":"O_NEGATIF","quantite":3,"urgence":"VITALE"}""";

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    HopitalRepository hopitaux;
    @Autowired
    DonneurRepository donneurs;

    @MockBean
    InventaireClient inventaire;
    @MockBean
    RegistreClient registre;
    @MockBean
    EmailNotificationPublisher notifications;

    private String jetonHopital;
    private String jetonDonneur;
    private Donneur donneurTest;

    @BeforeEach
    void preparer() {
        jetonHopital = "Bearer " + jwt.generer(hopitaux.save(Fixtures.hopital()));
        donneurTest = donneurs.save(Fixtures.donneur());
        jetonDonneur = "Bearer " + jwt.generer(donneurTest);
    }

    @Test
    void sansJeton_renvoie401() throws Exception {
        mvc.perform(post("/api/v1/demandes").contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void jetonDeDonneur_renvoie403() throws Exception {
        mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isForbidden());
    }

    @Test
    void quantiteNulle_renvoie400AvecLeChampEnErreur() throws Exception {
        mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON)
                        .content("{\"groupeSanguin\":\"O_NEGATIF\",\"quantite\":0,\"urgence\":\"VITALE\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erreurs.quantite").exists());
    }

    @Test
    void demandeServieParDeuxBanques_renvoie201() throws Exception {
        when(inventaire.reserver(eq(List.of(O_NEGATIF)), eq(3), anyString())).thenReturn(List.of(
                new AllocationObtenue("BANQUE-A", O_NEGATIF, 1),
                new AllocationObtenue("BANQUE-B", O_NEGATIF, 2)));

        mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.statut").value("ACCEPTEE"))
                .andExpect(jsonPath("$.allocations.length()").value(2));
    }

    @Test
    void stockInsuffisant_demandeEnAttente() throws Exception {
        when(inventaire.reserver(anyList(), anyInt(), anyString()))
                .thenThrow(new StockInsuffisantException("3 poches demandées, 1 disponible"));

        mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
    }

    @Test
    void stockInsuffisant_solliciteSeulementLesDonneursEligiblesAyantAccepteLesAlertes() throws Exception {
        donneurTest.setAccepteAlertesDon(true);
        donneurs.save(donneurTest);
        when(inventaire.reserver(anyList(), anyInt(), anyString()))
                .thenThrow(new StockInsuffisantException("3 poches demandées, 1 disponible"));
        when(registre.listerDonneursEligibles(O_NEGATIF))
                .thenReturn(List.of(donneurTest.getIdentifiantNational()));

        mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));

        verify(notifications).publierApresCommit(argThat(event ->
                event.type() == EmailNotificationEvent.Type.DONNEUR_SOLLICITE
                        && event.recipientEmail().equals(donneurTest.getEmail())));
    }

    @Test
    void preferenceAlertePeutEtreDesactiveeParLeDonneurConnecte() throws Exception {
        donneurTest.setAccepteAlertesDon(true);
        donneurs.save(donneurTest);

        mvc.perform(patch("/api/v1/donneurs/{id}/alertes-email", donneurTest.getId())
                        .header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON)
                        .content("{\"accepter\":false}"))
                .andExpect(status().isNoContent());

        org.junit.jupiter.api.Assertions.assertFalse(
                donneurs.findById(donneurTest.getId()).orElseThrow().isAccepteAlertesDon());
    }

    @Test
    void annulerDeuxFois_renvoie409() throws Exception {
        when(inventaire.reserver(anyList(), anyInt(), anyString()))
                .thenReturn(List.of(new AllocationObtenue("BANQUE-A", O_NEGATIF, 3)));
        MvcResult creation = mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andExpect(status().isCreated())
                .andReturn();
        Integer id = JsonPath.read(creation.getResponse().getContentAsString(), "$.id");

        mvc.perform(patch("/api/v1/demandes/{id}/annulation", id).header(AUTHORIZATION, jetonHopital))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statut").value("ANNULEE"));
        verify(inventaire).liberer(startsWith("DEM-"));

        mvc.perform(patch("/api/v1/demandes/{id}/annulation", id).header(AUTHORIZATION, jetonHopital))
                .andExpect(status().isConflict());
    }

    @Test
    void demandeDUnAutreHopital_renvoie404() throws Exception {
        String autreHopital = "Bearer " + jwt.generer(hopitaux.save(Fixtures.hopital()));
        when(inventaire.reserver(anyList(), anyInt(), anyString())).thenReturn(List.of());
        MvcResult creation = mvc.perform(post("/api/v1/demandes").header(AUTHORIZATION, jetonHopital)
                        .contentType(APPLICATION_JSON).content(DEMANDE))
                .andReturn();
        Integer id = JsonPath.read(creation.getResponse().getContentAsString(), "$.id");

        mvc.perform(patch("/api/v1/demandes/{id}/annulation", id).header(AUTHORIZATION, autreHopital))
                .andExpect(status().isNotFound());
    }
}
