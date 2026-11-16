package com.donsang.dons.erreur;

/** SOAP Fault « donneur inconnu » renvoyé par le Registre ; traduite en 404. */
public class DonneurInconnuException extends RessourceIntrouvableException {

    public DonneurInconnuException(String identifiantNational) {
        super("Donneur inconnu du registre national : " + identifiantNational);
    }
}
