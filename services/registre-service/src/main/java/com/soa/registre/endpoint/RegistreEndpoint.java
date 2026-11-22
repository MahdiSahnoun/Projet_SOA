package com.soa.registre.endpoint;

import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import com.soa.registre.service.RegistreService;

import com.donsang.registre.v1.EnregistrerDonRequest;
import com.donsang.registre.v1.EnregistrerDonResponse;
import com.donsang.registre.v1.ListerDonneursEligiblesRequest;
import com.donsang.registre.v1.ListerDonneursEligiblesResponse;
import com.donsang.registre.v1.VerifierEligibiliteRequest;
import com.donsang.registre.v1.VerifierEligibiliteResponse;
import com.donsang.registre.v1.VerifierExistenceRequest;
import com.donsang.registre.v1.VerifierExistenceResponse;

import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoint SOAP — Registre national des donneurs.
 *
 * Namespace : http://donsang.com/registre/v1  (identique au XSD de mahdi)
 *
 * Opérations :
 *   T04 — VerifierExistence        (identifiantNational → existe)
 *   T08 — VerifierEligibilite      (identifiantNational → eligible / SOAP Fault)
 *   T08 — EnregistrerDon           (identifiantNational + dateDon + groupe → numeroEnregistrement)
 *   T15 — ListerDonneursEligibles  (groupe → liste identifiantNational)
 */
@Endpoint
public class RegistreEndpoint {

    private static final String NAMESPACE = "http://donsang.com/registre/v1";

    private final RegistreService registreService;

    public RegistreEndpoint(RegistreService registreService) {
        this.registreService = registreService;
    }

    // ----------------------------------------------------------------
    // T04 — VerifierExistence
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "VerifierExistenceRequest")
    @ResponsePayload
    public VerifierExistenceResponse verifierExistence(
            @RequestPayload VerifierExistenceRequest request) {

        boolean existe = registreService.verifierExistence(request.getIdentifiantNational());

        VerifierExistenceResponse response = new VerifierExistenceResponse();
        response.setExiste(existe);
        return response;
    }

    // ----------------------------------------------------------------
    // T08 — VerifierEligibilite
    // Lève DonneurInconnuException ou DonneurInelegibleException si KO.
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "VerifierEligibiliteRequest")
    @ResponsePayload
    public VerifierEligibiliteResponse verifierEligibilite(
            @RequestPayload VerifierEligibiliteRequest request) {

        // Lève une SOAP Fault CLIENT si inconnu ou inéligible
        registreService.verifierEligibilite(request.getIdentifiantNational());

        VerifierEligibiliteResponse response = new VerifierEligibiliteResponse();
        response.setEligible(true);
        return response;
    }

    // ----------------------------------------------------------------
    // T08 — EnregistrerDon
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "EnregistrerDonRequest")
    @ResponsePayload
    public EnregistrerDonResponse enregistrerDon(
            @RequestPayload EnregistrerDonRequest request) {

        registreService.enregistrerDon(request.getIdentifiantNational());

        EnregistrerDonResponse response = new EnregistrerDonResponse();
        // Numéro d'enregistrement : identifiant + date du jour
        response.setNumeroEnregistrement("REG-" + request.getIdentifiantNational()
                + "-" + LocalDate.now());
        return response;
    }

    // ----------------------------------------------------------------
    // T15 — ListerDonneursEligibles
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "ListerDonneursEligiblesRequest")
    @ResponsePayload
    public ListerDonneursEligiblesResponse listerDonneursEligibles(
            @RequestPayload ListerDonneursEligiblesRequest request) {

        GroupeSanguin groupe = GroupeSanguin.valueOf(request.getGroupe());

        List<Donneur> donneurs = registreService.listerDonneursEligibles(groupe);

        ListerDonneursEligiblesResponse response = new ListerDonneursEligiblesResponse();
        donneurs.forEach(d -> response.getIdentifiantNational().add(d.getId()));
        return response;
    }
}
