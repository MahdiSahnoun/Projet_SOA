package com.donsang.dons.securite;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Lit « Authorization: Bearer <jeton> » et authentifie la requête.
 * Volontairement pas un @Component : sinon Spring Boot l'enregistrerait une seconde fois
 * comme filtre de servlet, hors de la chaîne de sécurité.
 */
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest requete, HttpServletResponse reponse, FilterChain chaine)
            throws ServletException, IOException {
        String entete = requete.getHeader(HttpHeaders.AUTHORIZATION);
        if (entete != null && entete.startsWith(PREFIXE)) {
            try {
                Claims claims = jwtService.lire(entete.substring(PREFIXE.length()));
                String role = claims.get(JwtService.CLAIM_ROLE, String.class);
                var authentification = new UsernamePasswordAuthenticationToken(
                        claims.getSubject(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role)));
                SecurityContextHolder.getContext().setAuthentication(authentification);
            } catch (JwtException | IllegalArgumentException e) {
                // Jeton invalide : la requête reste anonyme et finira en 401 si la route est protégée.
                SecurityContextHolder.clearContext();
            }
        }
        chaine.doFilter(requete, reponse);
    }
}
