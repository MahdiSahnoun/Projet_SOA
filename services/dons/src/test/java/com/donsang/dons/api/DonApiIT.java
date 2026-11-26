package com.donsang.dons.api;

import static com.donsang.dons.domaine.GroupeSanguin.O_NEGATIF;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.donsang.dons.Fixtures;
import com.donsang.dons.client.Eligibilite;
import com.donsang.dons.client.InventaireClient;
import com.donsang.dons.client.RegistreClient;
import com.donsang.dons.depot.BanqueDeSangRepository;
import com.donsang.dons.depot.DonneurRepository;
import com.donsang.dons.securite.JwtService;
import com.donsang.dons.notification.EmailNotificationPublisher;

/** Parcours d'un don (REST -> SOAP -> gRPC) avec Registre et Inventaire simulés. */
// Pas de Config Server ni d'Eureka pendant les tests (sinon dons.yml remplace la base H2 du profil test)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DonApiIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    JwtService jwt;
    @Autowired
    DonneurRepository donneurs;
    @Autowired
    BanqueDeSangRepository banques;

    @MockBean
    InventaireClient inventaire;
    @MockBean
    RegistreClient registre;
    @MockBean
    EmailNotificationPublisher notifications;

    private String jetonDonneur;
    private String codeBanque;

    @BeforeEach
    void preparer() {
        jetonDonneur = "Bearer " + jwt.generer(donneurs.save(Fixtures.donneur()));
        codeBanque = banques.save(Fixtures.banque()).getCodeBanque();
    }

    private String corps() {
        return "{\"codeBanque\":\"" + codeBanque + "\"}";
    }

    @Test
    void donEligible_estAccepte_etLaPocheEstAjoutee() throws Exception {
        when(registre.verifier(anyString())).thenReturn(Eligibilite.eligible());
        when(registre.enregistrerDon(anyString(), any(), any())).thenReturn("REG-2026-001");

        mvc.perform(post("/api/v1/dons").header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("ACCEPTEE"))
                .andExpect(jsonPath("$.numeroEnregistrement").value("REG-2026-001"));
        verify(inventaire).ajouterPoche(eq(codeBanque), eq(O_NEGATIF), eq(450), any(), any());
    }

    @Test
    void donneurIneligible_donRefuse() throws Exception {
        when(registre.verifier(anyString()))
                .thenReturn(Eligibilite.refusee("Délai minimum entre deux dons non respecté"));

        mvc.perform(post("/api/v1/dons").header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON).content(corps()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statut").value("REFUSEE"));
        verifyNoInteractions(inventaire);
    }

    @Test
    void registreIndisponible_renvoie202EtDonEnAttente() throws Exception {
        when(registre.verifier(anyString()))
                .thenReturn(Eligibilite.enAttente("Registre indisponible : vérification d'éligibilité différée"));

        mvc.perform(post("/api/v1/dons").header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON).content(corps()))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.statut").value("EN_ATTENTE"));
        verifyNoInteractions(inventaire);
    }

    @Test
    void banqueInconnue_renvoie404() throws Exception {
        mvc.perform(post("/api/v1/dons").header(AUTHORIZATION, jetonDonneur)
                        .contentType(APPLICATION_JSON).content("{\"codeBanque\":\"BANQUE-INEXISTANTE\"}"))
                .andExpect(status().isNotFound());
    }
}
