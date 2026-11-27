package com.soa.registre.exception;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * T09 — SOAP Fault renvoyé quand l'identifiant national n'existe pas dans registre_db.
 *
 * faultCode = Client et faultString = message de l'exception (pas de faultStringOrReason fixe).
 * Le client SOAP de Dons reconnaît ce cas parce que le message commence par
 * "Donneur inconnu" : il le traduit en HTTP 404.
 */
@SoapFault(faultCode = FaultCode.CLIENT)
public class DonneurInconnuException extends RuntimeException {

    public DonneurInconnuException(String identifiantNational) {
        super("Donneur inconnu du registre national : " + identifiantNational);
    }
}
