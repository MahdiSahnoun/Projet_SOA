package com.donsang.dons.erreur;

/** Un service distant est en panne ou a dépassé son délai ; traduite en 503. */
public class ServiceIndisponibleException extends RuntimeException {

    public ServiceIndisponibleException(String message, Throwable cause) {
        super(message, cause);
    }
}
