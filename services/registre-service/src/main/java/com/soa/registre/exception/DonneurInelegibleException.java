package com.soa.registre.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * T09 — SOAP Fault renvoyé par EnregistrerDon quand le donneur existe mais ne remplit
 * pas les critères d'éligibilité (âge, délai, poids, contre-indication).
 *
 * faultCode = Client. Le client Dons traite tout Fault Client qui ne commence pas par
 * "Donneur inconnu" comme un conflit (HTTP 409) et affiche ce message tel quel.
 *
 * VerifierEligibilite ne lève PAS ce Fault : il répond eligible=false + motif.
 */
@SoapFault(faultCode = FaultCode.CLIENT)
public class DonneurInelegibleException extends RuntimeException {

    public DonneurInelegibleException(String identifiantNational, String motif) {
        super("Donneur inéligible : " + identifiantNational + " — " + motif);
    }
}
