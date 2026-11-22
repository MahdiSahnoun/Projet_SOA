package com.donsang.dons.securite;

import org.springframework.security.core.Authentication;

/** Le « subject » du JWT est l'identifiant de l'utilisateur. */
public final class UtilisateurConnecte {

    private UtilisateurConnecte() {
    }

    public static Long id(Authentication authentification) {
        return Long.valueOf(authentification.getName());
    }
}
