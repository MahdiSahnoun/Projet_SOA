package com.donsang.dons.api;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.donsang.dons.Fixtures;
import com.donsang.dons.client.InventaireClient;
import com.donsang.dons.client.RegistreClient;
import com.donsang.dons.depot.HopitalRepository;
import com.donsang.dons.domaine.Hopital;
import com.donsang.dons.notification.EmailNotificationPublisher;

// Pas de Config Server ni d'Eureka pendant les tests (sinon dons.yml remplace la base H2 du profil test)
@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIT {

    @Autowired
    MockMvc mvc;
    @Autowired
    HopitalRepository hopitaux;
    @Autowired
    PasswordEncoder passwordEncoder;

    @MockBean
    InventaireClient inventaire;
    @MockBean
    RegistreClient registre;
    @MockBean
    EmailNotificationPublisher notifications;

    @Test
    void connexionValide_renvoieUnJeton() throws Exception {
        Hopital h = Fixtures.hopital();
        h.setMotDePasse(passwordEncoder.encode("MotDePasse-Test-1"));
        hopitaux.save(h);

        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + h.getEmail() + "\",\"motDePasse\":\"MotDePasse-Test-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void mauvaisMotDePasse_renvoie401() throws Exception {
        Hopital h = Fixtures.hopital();
        h.setMotDePasse(passwordEncoder.encode("MotDePasse-Test-1"));
        hopitaux.save(h);

        mvc.perform(post("/api/v1/auth/login").contentType(APPLICATION_JSON)
                        .content("{\"email\":\"" + h.getEmail() + "\",\"motDePasse\":\"faux\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inscriptionDonneur_publique_renvoie201SansMotDePasse() throws Exception {
        when(registre.existe(anyString())).thenReturn(true);
        String suffixe = UUID.randomUUID().toString().substring(0, 8);

        mvc.perform(post("/api/v1/donneurs").contentType(APPLICATION_JSON).content("""
                        {"nom":"Sami","email":"sami-%s@test.tn","motDePasse":"MotDePasse-Test-1",
                         "identifiantNational":"TN-%s","dateNaissance":"1994-05-12",
                         "poids":70,"groupeSanguin":"A_POSITIF"}""".formatted(suffixe, suffixe)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist());
    }

    @Test
    void inscriptionDonneurMineur_renvoie400() throws Exception {
        when(registre.existe(anyString())).thenReturn(true);
        String suffixe = UUID.randomUUID().toString().substring(0, 8);
        String naissance = java.time.LocalDate.now().minusYears(16).toString();

        mvc.perform(post("/api/v1/donneurs").contentType(APPLICATION_JSON).content("""
                        {"nom":"Ali","email":"ali-%s@test.tn","motDePasse":"MotDePasse-Test-1",
                         "identifiantNational":"TN-%s","dateNaissance":"%s",
                         "poids":60,"groupeSanguin":"B_POSITIF"}""".formatted(suffixe, suffixe, naissance)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupDonneur_publique_renvoie201SansMotDePasseNiJeton() throws Exception {
        when(registre.existe(anyString())).thenReturn(true);
        String suffixe = UUID.randomUUID().toString().substring(0, 8);

        mvc.perform(post("/api/v1/auth/signup").contentType(APPLICATION_JSON).content("""
                        {"nom":"Nour","email":"nour-%s@test.tn","motDePasse":"MotDePasse-Test-1",
                         "identifiantNational":"TN-%s","dateNaissance":"1994-05-12",
                         "poids":70,"groupeSanguin":"O_NEGATIF"}""".formatted(suffixe, suffixe)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.motDePasse").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
}
