package com.donsang.dons.client;

import java.time.LocalDate;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.springframework.stereotype.Service;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.erreur.ConflitException;
import com.donsang.dons.erreur.DonneurInconnuException;
import com.donsang.dons.erreur.RegistreIndisponibleException;
import com.donsang.registre.v1.EnregistrerDonRequest;
import com.donsang.registre.v1.EnregistrerDonResponse;
import com.donsang.registre.v1.VerifierEligibiliteRequest;
import com.donsang.registre.v1.VerifierEligibiliteResponse;
import com.donsang.registre.v1.VerifierExistenceRequest;
import com.donsang.registre.v1.VerifierExistenceResponse;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

/**
 * Client SOAP du Registre national (appel REST -> SOAP via WebServiceTemplate).
 * Les SOAP Faults métier sont traduits en exceptions du domaine ;
 * une panne déclenche la méthode de repli.
 */
@Service
public class RegistreClient {

    static final String REGISTRE = "registre";

    private final WebServiceTemplate registreTemplate;

    public RegistreClient(WebServiceTemplate registreTemplate) {
        this.registreTemplate = registreTemplate;
    }

    @CircuitBreaker(name = REGISTRE, fallbackMethod = "repliExistence")
    public boolean existe(String identifiantNational) {
        VerifierExistenceRequest requete = new VerifierExistenceRequest();
        requete.setIdentifiantNational(identifiantNational);
        VerifierExistenceResponse reponse = (VerifierExistenceResponse) registreTemplate.marshalSendAndReceive(requete);
        return reponse.isExiste();
    }

    @CircuitBreaker(name = REGISTRE, fallbackMethod = "repliEligibilite")
    public Eligibilite verifier(String identifiantNational) {
        VerifierEligibiliteRequest requete = new VerifierEligibiliteRequest();
        requete.setIdentifiantNational(identifiantNational);
        try {
            VerifierEligibiliteResponse reponse =
                    (VerifierEligibiliteResponse) registreTemplate.marshalSendAndReceive(requete);
            return reponse.isEligible() ? Eligibilite.eligible() : Eligibilite.refusee(reponse.getMotif());
        } catch (SoapFaultClientException fault) {
            throw traduire(fault, identifiantNational);
        }
    }

    @CircuitBreaker(name = REGISTRE, fallbackMethod = "repliEnregistrement")
    public String enregistrerDon(String identifiantNational, LocalDate dateDon, GroupeSanguin groupe) {
        EnregistrerDonRequest requete = new EnregistrerDonRequest();
        requete.setIdentifiantNational(identifiantNational);
        requete.setDateDon(versXml(dateDon));
        requete.setGroupe(groupe.name());
        try {
            EnregistrerDonResponse reponse = (EnregistrerDonResponse) registreTemplate.marshalSendAndReceive(requete);
            return reponse.getNumeroEnregistrement();
        } catch (SoapFaultClientException fault) {
            throw traduire(fault, identifiantNational);
        }
    }

    // --- Méthodes de repli ---------------------------------------------------------------

    /** Registre injoignable à l'inscription : on accepte, l'éligibilité sera revérifiée au moment du don. */
    private boolean repliExistence(String identifiantNational, Throwable erreur) {
        return true;
    }

    private Eligibilite repliEligibilite(String identifiantNational, Throwable erreur) {
        if (estMetier(erreur)) {
            throw (RuntimeException) erreur;   // erreur métier : on la propage telle quelle
        }
        return Eligibilite.enAttente("Registre indisponible : vérification d'éligibilité différée");
    }

    private String repliEnregistrement(String identifiantNational, LocalDate dateDon, GroupeSanguin groupe,
                                       Throwable erreur) {
        if (estMetier(erreur)) {
            throw (RuntimeException) erreur;
        }
        throw new RegistreIndisponibleException(erreur);
    }

    private static boolean estMetier(Throwable erreur) {
        return erreur instanceof DonneurInconnuException || erreur instanceof ConflitException;
    }

    /**
     * Fault « Client » = refus métier (donneur inconnu, don refusé).
     * Fault « Server » = panne du Registre : on la laisse remonter pour que le circuit breaker la compte.
     */
    private static RuntimeException traduire(SoapFaultClientException fault, String identifiantNational) {
        String code = fault.getFaultCode() != null ? fault.getFaultCode().getLocalPart() : "";
        if (!"Client".equals(code)) {
            return fault;
        }
        String raison = fault.getFaultStringOrReason();
        if (raison != null && raison.startsWith("Donneur inconnu")) {
            return new DonneurInconnuException(identifiantNational);
        }
        return new ConflitException(raison != null ? raison : "Don refusé par le registre national");
    }

    private static XMLGregorianCalendar versXml(LocalDate date) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(date.toString());
    }
}
