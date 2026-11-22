package com.soa.registre.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * SOAP Fault Client renvoyé quand le texte "groupe" reçu ne correspond à aucun
 * groupe sanguin connu (attendu : O_NEGATIF, O_POSITIF, ..., AB_POSITIF).
 */
@SoapFault(faultCode = FaultCode.CLIENT)
public class GroupeSanguinInvalideException extends RuntimeException {

    public GroupeSanguinInvalideException(String valeur, String valeursAttendues) {
        super("Groupe sanguin invalide : '" + valeur + "'. Valeurs attendues : " + valeursAttendues);
    }
}
