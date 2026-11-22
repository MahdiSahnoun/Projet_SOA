package com.soa.registre.endpoint;

import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import com.soa.registre.generated.*;
import com.soa.registre.service.RegistreService;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import java.util.List;

/**
 * Endpoint SOAP — Registre.
 *
 * Namespace : http://soa.com/registre  (correspond à targetNamespace de registre.xsd)
 *
 * Opérations :
 *   T04 — VerifierExistence
 *   T08 — VerifierEligibilite
 *   T08 — EnregistrerDon
 *   T15 — ListerDonneursEligibles
 *
 * La logique métier est dans RegistreService, pas ici.
 * Les exceptions DonneurInconnuException / DonneurInelegibleException sont annotées
 * @SoapFault et remontent automatiquement comme SOAP Fault CLIENT.
 */
@Endpoint
public class RegistreEndpoint {

    private static final String NAMESPACE = "http://soa.com/registre";

    private final RegistreService registreService;
    private final ObjectFactory objectFactory = new ObjectFactory();

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

        boolean existe = registreService.verifierExistence(request.getDonneurId());

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

        // Lève une exception SOAP Fault si inéligible ou inconnu
        registreService.verifierEligibilite(request.getDonneurId());

        VerifierEligibiliteResponse response = new VerifierEligibiliteResponse();
        response.setEligible(true);
        response.setMessage("Donneur éligible");
        return response;
    }

    // ----------------------------------------------------------------
    // T08 — EnregistrerDon
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "EnregistrerDonRequest")
    @ResponsePayload
    public EnregistrerDonResponse enregistrerDon(
            @RequestPayload EnregistrerDonRequest request) {

        registreService.enregistrerDon(request.getDonneurId());

        EnregistrerDonResponse response = new EnregistrerDonResponse();
        response.setSucces(true);
        response.setMessage("Don enregistré avec succès");
        return response;
    }

    // ----------------------------------------------------------------
    // T15 — ListerDonneursEligibles
    // ----------------------------------------------------------------

    @PayloadRoot(namespace = NAMESPACE, localPart = "ListerDonneursEligiblesRequest")
    @ResponsePayload
    public ListerDonneursEligiblesResponse listerDonneursEligibles(
            @RequestPayload ListerDonneursEligiblesRequest request) {

        GroupeSanguin groupe = GroupeSanguin.valueOf(request.getGroupeSanguin().value());

        List<Donneur> donneurs = registreService.listerDonneursEligibles(groupe);

        ListerDonneursEligiblesResponse response = new ListerDonneursEligiblesResponse();
        for (Donneur d : donneurs) {
            DonneurInfo info = new DonneurInfo();
            info.setId(d.getId());
            info.setNom(d.getNom());
            info.setPrenom(d.getPrenom());
            info.setTelephone(d.getTelephone());
            info.setGroupeSanguin(
                    com.soa.registre.generated.GroupeSanguin.fromValue(d.getGroupeSanguin().name()));
            response.getDonneurs().add(info);
        }
        return response;
    }
}
