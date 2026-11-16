package com.donsang.dons.service;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donsang.dons.api.dto.JetonReponse;
import com.donsang.dons.api.dto.LoginRequest;
import com.donsang.dons.depot.UtilisateurRepository;
import com.donsang.dons.domaine.Utilisateur;
import com.donsang.dons.erreur.IdentifiantsInvalidesException;
import com.donsang.dons.securite.JwtService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public JetonReponse connecter(LoginRequest requete) {
        Utilisateur utilisateur = utilisateurs.findByEmail(requete.email().toLowerCase(Locale.ROOT))
                .filter(u -> passwordEncoder.matches(requete.motDePasse(), u.getMotDePasse()))
                .orElseThrow(IdentifiantsInvalidesException::new);
        return new JetonReponse(jwtService.generer(utilisateur), "Bearer", jwtService.dureeSecondes());
    }
}
