package com.donsang.dons.erreur;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Toutes les erreurs de l'API au même format ProblemDetail (RFC 7807).
 * La classe parente traite déjà les erreurs Spring MVC standard (JSON illisible,
 * paramètre manquant ou mal typé, méthode non supportée...) avec le bon code HTTP.
 */
@RestControllerAdvice
public class GestionErreurs extends ResponseEntityExceptionHandler {

    /** 400 : détaille chaque champ invalide, ex. {"erreurs": {"quantite": "doit être supérieur ou égal à 1"}}. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail probleme = ex.getBody();
        probleme.setTitle("Requête invalide");
        probleme.setDetail("Un ou plusieurs champs sont invalides");
        Map<String, String> erreurs = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(e -> erreurs.putIfAbsent(e.getField(), e.getDefaultMessage()));
        probleme.setProperty("erreurs", erreurs);
        return handleExceptionInternal(ex, probleme, headers, status, request);
    }

    @ExceptionHandler(RequeteInvalideException.class)
    public ProblemDetail requeteInvalide(RequeteInvalideException e) {
        return probleme(HttpStatus.BAD_REQUEST, "Requête invalide", e.getMessage());
    }

    @ExceptionHandler(IdentifiantsInvalidesException.class)
    public ProblemDetail identifiantsInvalides(IdentifiantsInvalidesException e) {
        return probleme(HttpStatus.UNAUTHORIZED, "Authentification refusée", e.getMessage());
    }

    @ExceptionHandler(RessourceIntrouvableException.class)
    public ProblemDetail introuvable(RessourceIntrouvableException e) {
        return probleme(HttpStatus.NOT_FOUND, "Ressource introuvable", e.getMessage());
    }

    @ExceptionHandler({ ConflitException.class, StockInsuffisantException.class })
    public ProblemDetail conflit(RuntimeException e) {
        return probleme(HttpStatus.CONFLICT, "Conflit", e.getMessage());
    }

    @ExceptionHandler(ServiceIndisponibleException.class)
    public ProblemDetail indisponible(ServiceIndisponibleException e) {
        return probleme(HttpStatus.SERVICE_UNAVAILABLE, "Service indisponible", e.getMessage());
    }

    private static ProblemDetail probleme(HttpStatus statut, String titre, String detail) {
        ProblemDetail p = ProblemDetail.forStatusAndDetail(statut, detail);
        p.setTitle(titre);
        return p;
    }
}
