package com.soa.registre.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * T09 — SOAP Fault renvoyé quand le donneur existe mais ne remplit pas les
 * critères d'éligibilité (âge, délai, poids, contre-indication).
 *
 * faultCode = CLIENT — le client Dons l'interprète comme HTTP 409.
 * Le message commence par "Donneur inéligible" pour que le client puisse
 * distinguer ce cas de DonneurInconnuException (qui commence par "Donneur inconnu").
 */
@SoapFault(faultCode = FaultCode.CLIENT,
           faultStringOrReason = "Donneur inéligible")
public class DonneurInelegibleException extends RuntimeException {

    public DonneurInelegibleException(String donneurId, String raison) {
        super("Donneur inéligible : " + donneurId + " — " + raison);
    }
}
