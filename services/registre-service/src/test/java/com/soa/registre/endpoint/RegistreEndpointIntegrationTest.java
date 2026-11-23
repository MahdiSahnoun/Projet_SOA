package com.soa.registre.endpoint;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.ApplicationContext;
import org.springframework.ws.soap.SoapFault;
import org.springframework.ws.soap.SoapMessage;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.ResponseMatcher;
import org.springframework.xml.transform.StringSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.ws.test.server.RequestCreators.withPayload;
import static org.springframework.ws.test.server.ResponseMatchers.clientOrSenderFault;
import static org.springframework.ws.test.server.ResponseMatchers.noFault;
import static org.springframework.ws.test.server.ResponseMatchers.xpath;

/**
 * T13 — Tests d'intégration SOAP du Registre.
 *
 * Toute la chaîne est réelle : MessageDispatcher Spring-WS, endpoint, RegistreService,
 * JPA et un PostgreSQL lancé par Testcontainers, rempli par le vrai data.sql
 * (syntaxe Postgres : INTERVAL, ::date, ON CONFLICT — d'où Postgres et pas H2).
 *
 * Docker doit tourner ; sinon la classe est ignorée (disabledWithoutDocker).
 */
@SpringBootTest(properties = {
        // Pas de Config Server ni d'Eureka pendant les tests
        "spring.cloud.config.enabled=false",
        "eureka.client.enabled=false",
        // Mêmes réglages que registre-service.yml (servis normalement par le Config Server)
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.defer-datasource-initialization=true",
        "spring.sql.init.mode=always"
})
@Testcontainers(disabledWithoutDocker = true)
class RegistreEndpointIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final String NS = "http://donsang.com/registre/v1";
    private static final Map<String, String> PREFIXES = Map.of("r", NS);

    @Autowired
    private ApplicationContext applicationContext;

    private MockWebServiceClient client;

    @BeforeEach
    void creerClient() {
        client = MockWebServiceClient.createClient(applicationContext);
    }

    // ------------------------------------------------------------ VerifierEligibilite

    @Test
    void verifierEligibilite_donneurEligible() {
        client.sendRequest(withPayload(requete("VerifierEligibiliteRequest", "TN-00000001")))
                .andExpect(noFault())
                .andExpect(xpath("/r:VerifierEligibiliteResponse/r:eligible", PREFIXES).evaluatesTo("true"))
                .andExpect(xpath("/r:VerifierEligibiliteResponse/r:motif", PREFIXES).doesNotExist())
                .andExpect(xpath("/r:VerifierEligibiliteResponse/r:prochainDonPossible", PREFIXES).doesNotExist());
    }

    @Test
    void verifierEligibilite_donneurInconnu_faultClient() {
        client.sendRequest(withPayload(requete("VerifierEligibiliteRequest", "TN-99999999")))
                .andExpect(clientOrSenderFault())
                .andExpect(faultClientCommencantPar("Donneur inconnu"));
    }

    /** Contrat Dons : un inéligible n'est PAS un Fault ici, mais eligible=false + motif. */
    @Test
    void verifierEligibilite_donRecent_ineligibleAvecMotifEtProchainDon() {
        String dans36Jours = LocalDate.now().plusDays(36).toString();   // don il y a 20 j + 56 j

        client.sendRequest(withPayload(requete("VerifierEligibiliteRequest", "TN-00000011")))
                .andExpect(noFault())
                .andExpect(xpath("/r:VerifierEligibiliteResponse/r:eligible", PREFIXES).evaluatesTo("false"))
                .andExpect(xpath("starts-with(/r:VerifierEligibiliteResponse/r:motif, 'délai insuffisant')", PREFIXES)
                        .evaluatesTo(true))
                .andExpect(xpath("/r:VerifierEligibiliteResponse/r:prochainDonPossible", PREFIXES)
                        .evaluatesTo(dans36Jours));
    }

    // ------------------------------------------------------------ EnregistrerDon

    /** Le Fault DonneurInelegible existe sur EnregistrerDon (Dons le traduit en 409). */
    @Test
    void enregistrerDon_contreIndication_faultDonneurInelegible() {
        client.sendRequest(withPayload(enregistrerDon("TN-00000014", "O_NEGATIF")))
                .andExpect(clientOrSenderFault())
                .andExpect(faultClientCommencantPar("Donneur inéligible"));
    }

    /** Donneur réservé à ce test (TN-00000003, A_NEGATIF) : sa mise à jour ne gêne pas les autres cas. */
    @Test
    void enregistrerDon_eligible_renvoieUnNumero() {
        String numero = "REG-TN-00000003-" + LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        client.sendRequest(withPayload(enregistrerDon("TN-00000003", "A_NEGATIF")))
                .andExpect(noFault())
                .andExpect(xpath("/r:EnregistrerDonResponse/r:numeroEnregistrement", PREFIXES).evaluatesTo(numero));
    }

    // ------------------------------------------------------------ VerifierExistence

    @Test
    void verifierExistence_donneurConnu() {
        client.sendRequest(withPayload(requete("VerifierExistenceRequest", "TN-00000001")))
                .andExpect(noFault())
                .andExpect(xpath("/r:VerifierExistenceResponse/r:existe", PREFIXES).evaluatesTo("true"));
    }

    @Test
    void verifierExistence_donneurInconnu_falseSansFault() {
        client.sendRequest(withPayload(requete("VerifierExistenceRequest", "TN-99999999")))
                .andExpect(noFault())
                .andExpect(xpath("/r:VerifierExistenceResponse/r:existe", PREFIXES).evaluatesTo("false"));
    }

    // ------------------------------------------------------------ ListerDonneursEligibles

    @Test
    void listerDonneursEligibles_oNegatif() {
        String payload = "<r:ListerDonneursEligiblesRequest xmlns:r='" + NS + "'>"
                + "<r:groupe>O_NEGATIF</r:groupe>"
                + "</r:ListerDonneursEligiblesRequest>";

        client.sendRequest(withPayload(new StringSource(payload)))
                .andExpect(noFault())
                .andExpect(xpath("//r:identifiantNational[. = 'TN-00000001']", PREFIXES).exists())
                .andExpect(xpath("//r:identifiantNational[. = 'TN-00000009']", PREFIXES).exists())
                // O_NEGATIF mais contre-indication
                .andExpect(xpath("//r:identifiantNational[. = 'TN-00000014']", PREFIXES).doesNotExist())
                // inéligible (et d'un autre groupe)
                .andExpect(xpath("//r:identifiantNational[. = 'TN-00000011']", PREFIXES).doesNotExist());
    }

    // ------------------------------------------------------------ Outils

    private static StringSource requete(String element, String identifiantNational) {
        return new StringSource("<r:" + element + " xmlns:r='" + NS + "'>"
                + "<r:identifiantNational>" + identifiantNational + "</r:identifiantNational>"
                + "</r:" + element + ">");
    }

    private static StringSource enregistrerDon(String identifiantNational, String groupe) {
        return new StringSource("<r:EnregistrerDonRequest xmlns:r='" + NS + "'>"
                + "<r:identifiantNational>" + identifiantNational + "</r:identifiantNational>"
                + "<r:dateDon>" + LocalDate.now() + "</r:dateDon>"
                + "<r:groupe>" + groupe + "</r:groupe>"
                + "</r:EnregistrerDonRequest>");
    }

    /**
     * Vérifie exactement ce que lit le client SOAP de Dons :
     * faultCode de partie locale "Client" et faultString qui commence par le préfixe donné.
     */
    private static ResponseMatcher faultClientCommencantPar(String prefixe) {
        return (request, response) -> {
            SoapFault fault = ((SoapMessage) response).getSoapBody().getFault();
            assertThat(fault).as("SOAP Fault attendu").isNotNull();
            assertThat(fault.getFaultCode().getLocalPart()).isEqualTo("Client");
            assertThat(fault.getFaultStringOrReason()).startsWith(prefixe);
        };
    }
}
