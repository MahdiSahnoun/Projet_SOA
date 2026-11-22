package com.soa.registre.endpoint;

import com.donsang.registre.v1.EnregistrerDonRequest;
import com.donsang.registre.v1.EnregistrerDonResponse;
import com.donsang.registre.v1.ListerDonneursEligiblesDetailRequest;
import com.donsang.registre.v1.ListerDonneursEligiblesDetailResponse;
import com.donsang.registre.v1.ListerDonneursEligiblesRequest;
import com.donsang.registre.v1.ListerDonneursEligiblesResponse;
import com.donsang.registre.v1.VerifierEligibiliteRequest;
import com.donsang.registre.v1.VerifierEligibiliteResponse;
import com.donsang.registre.v1.VerifierExistenceRequest;
import com.donsang.registre.v1.VerifierExistenceResponse;
import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import com.soa.registre.service.RegistreService;
import com.soa.registre.service.ResultatEligibilite;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalDate;

/**
 * Endpoint SOAP — Registre national.
 *
 * Contrat : registre.xsd partagé avec le client SOAP du service Dons
 * (namespace http://donsang.com/registre/v1, classes JAXB com.donsang.registre.v1).
 *
 * Opérations :
 *   T04 — VerifierExistence        : existe (jamais de Fault)
 *   T08 — VerifierEligibilite      : eligible, motif, prochainDonPossible ; Fault si inconnu
 *   T08 — EnregistrerDon           : numeroEnregistrement ; Fault si inconnu ou inéligible
 *   T15 — ListerDonneursEligibles  : identifiants nationaux éligibles d'un groupe
 *   T15 — ListerDonneursEligiblesDetail : idem + nom, prénom, téléphone (appel aux donneurs, T16)
 *
 * Ici on ne fait que traduire XML <-> Java : la logique métier est dans RegistreService.
 * Les exceptions annotées @SoapFault deviennent des SOAP Faults "Client".
 */
@Endpoint
public class RegistreEndpoint {

    static final String NAMESPACE = "http://donsang.com/registre/v1";

    private final RegistreService registreService;

    public RegistreEndpoint(RegistreService registreService) {
        this.registreService = registreService;
    }

    // ----------------------------------------------------------------
    // T04 — VerifierExistence
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "VerifierExistenceRequest")
    @ResponsePayload
    public VerifierExistenceResponse verifierExistence(@RequestPayload VerifierExistenceRequest request) {
        VerifierExistenceResponse response = new VerifierExistenceResponse();
        response.setExiste(registreService.verifierExistence(request.getIdentifiantNational()));
        return response;
    }

    // ----------------------------------------------------------------
    // T08 — VerifierEligibilite
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "VerifierEligibiliteRequest")
    @ResponsePayload
    public VerifierEligibiliteResponse verifierEligibilite(@RequestPayload VerifierEligibiliteRequest request) {
        ResultatEligibilite resultat = registreService.verifierEligibilite(request.getIdentifiantNational());

        VerifierEligibiliteResponse response = new VerifierEligibiliteResponse();
        response.setEligible(resultat.eligible());
        response.setMotif(resultat.motif());   // null si éligible : élément absent (minOccurs=0)
        if (resultat.prochainDonPossible() != null) {
            response.setProchainDonPossible(versXml(resultat.prochainDonPossible()));
        }
        return response;
    }

    // ----------------------------------------------------------------
    // T08 — EnregistrerDon
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "EnregistrerDonRequest")
    @ResponsePayload
    public EnregistrerDonResponse enregistrerDon(@RequestPayload EnregistrerDonRequest request) {
        registreService.groupeSanguin(request.getGroupe());   // Fault Client si groupe inconnu
        String numero = registreService.enregistrerDon(request.getIdentifiantNational(),
                versLocalDate(request.getDateDon()));

        EnregistrerDonResponse response = new EnregistrerDonResponse();
        response.setNumeroEnregistrement(numero);
        return response;
    }

    // ----------------------------------------------------------------
    // T15 — ListerDonneursEligibles
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "ListerDonneursEligiblesRequest")
    @ResponsePayload
    public ListerDonneursEligiblesResponse listerDonneursEligibles(
            @RequestPayload ListerDonneursEligiblesRequest request) {

        GroupeSanguin groupe = registreService.groupeSanguin(request.getGroupe());

        ListerDonneursEligiblesResponse response = new ListerDonneursEligiblesResponse();
        for (Donneur d : registreService.listerDonneursEligibles(groupe)) {
            response.getIdentifiantNational().add(d.getId());
        }
        return response;
    }

    // ----------------------------------------------------------------
    // T15 / T16 — ListerDonneursEligiblesDetail : même filtre, avec coordonnées
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "ListerDonneursEligiblesDetailRequest")
    @ResponsePayload
    public ListerDonneursEligiblesDetailResponse listerDonneursEligiblesDetail(
            @RequestPayload ListerDonneursEligiblesDetailRequest request) {

        GroupeSanguin groupe = registreService.groupeSanguin(request.getGroupe());

        ListerDonneursEligiblesDetailResponse response = new ListerDonneursEligiblesDetailResponse();
        for (Donneur d : registreService.listerDonneursEligibles(groupe)) {
            ListerDonneursEligiblesDetailResponse.Donneur info = new ListerDonneursEligiblesDetailResponse.Donneur();
            info.setIdentifiantNational(d.getId());
            info.setNom(d.getNom());
            info.setPrenom(d.getPrenom());
            info.setTelephone(d.getTelephone());
            response.getDonneur().add(info);
        }
        return response;
    }

    // ----------------------------------------------------------------
    // Conversion xs:date <-> LocalDate
    // ----------------------------------------------------------------

    private static XMLGregorianCalendar versXml(LocalDate date) {
        return DatatypeFactory.newDefaultInstance().newXMLGregorianCalendar(date.toString());
    }

    private static LocalDate versLocalDate(XMLGregorianCalendar date) {
        if (date == null) {
            return LocalDate.now();
        }
        return LocalDate.of(date.getYear(), date.getMonth(), date.getDay());
    }
}
