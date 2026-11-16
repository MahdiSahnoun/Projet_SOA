package com.donsang.dons.securite;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.donsang.dons.domaine.Utilisateur;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Émet et vérifie les jetons JWT signés en HMAC-SHA256.
 * Le secret vient de la variable d'environnement JWT_SECRET (au moins 32 caractères) :
 * un secret trop court fait échouer le démarrage, c'est voulu.
 */
@Service
public class JwtService {

    public static final String CLAIM_ROLE = "role";

    private final SecretKey cle;
    private final Duration duree;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.duree:PT2H}") Duration duree) {
        this.cle = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.duree = duree;
    }

    public String generer(Utilisateur utilisateur) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(utilisateur.getId()))
                .claim(CLAIM_ROLE, utilisateur.getRole().name())
                .issuedAt(Date.from(maintenant))
                .expiration(Date.from(maintenant.plus(duree)))
                .signWith(cle)
                .compact();
    }

    /** Lève une JwtException si le jeton est expiré, mal signé ou mal formé. */
    public Claims lire(String jeton) {
        return Jwts.parser().verifyWith(cle).build().parseSignedClaims(jeton).getPayload();
    }

    public long dureeSecondes() {
        return duree.toSeconds();
    }
}
