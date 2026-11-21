package com.donsang.dons.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.donsang.dons.api.dto.DonneurReponse;
import com.donsang.dons.api.dto.InscriptionDonneurRequest;
import com.donsang.dons.api.dto.JetonReponse;
import com.donsang.dons.api.dto.LoginRequest;
import com.donsang.dons.service.AuthService;
import com.donsang.dons.service.DonneurService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentification")
public class AuthController {

    private final AuthService authService;
    private final DonneurService donneurService;

    /** 200 + jeton ; 401 si email ou mot de passe incorrect. */
    @PostMapping("/login")
    public JetonReponse login(@Valid @RequestBody LoginRequest requete) {
        return authService.connecter(requete);
    }

    /** Alias d'inscription donneur publique : 201 + Location, sans mot de passe ni jeton. */
    @PostMapping("/signup")
    public ResponseEntity<DonneurReponse> signup(@Valid @RequestBody InscriptionDonneurRequest requete,
                                                 UriComponentsBuilder uri) {
        DonneurReponse donneur = donneurService.inscrire(requete);
        URI location = uri.path("/api/v1/donneurs/{id}").buildAndExpand(donneur.id()).toUri();
        return ResponseEntity.created(location).body(donneur);
    }
}
