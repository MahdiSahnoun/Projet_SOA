package com.soa.registre.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * T09 — SOAP Fault renvoyé quand l'identifiant donneur n'existe pas dans registre_db.
 *
 * faultCode = CLIENT  (attendu par le client SOAP du service Dons)
 * Le message commence obligatoirement par "Donneur inconnu" pour que le client
 * Dons le reconnaisse et renvoie un HTTP 404.
 */
@SoapFault(faultCode = FaultCode.CLIENT,
           faultStringOrReason = "Donneur inconnu : identifiant introuvable dans le registre")
public class DonneurInconnuException extends RuntimeException {

    public DonneurInconnuException(String donneurId) {
        super("Donneur inconnu : " + donneurId);
    }
}
