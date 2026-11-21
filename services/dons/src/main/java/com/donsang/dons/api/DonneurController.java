package com.donsang.dons.api;

import java.net.URI;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.donsang.dons.api.dto.DonneurReponse;
import com.donsang.dons.api.dto.InscriptionDonneurRequest;
import com.donsang.dons.securite.UtilisateurConnecte;
import com.donsang.dons.service.DonneurService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/donneurs")
@RequiredArgsConstructor
@Tag(name = "Donneurs")
public class DonneurController {

    private final DonneurService donneurService;

    /** Inscription publique : 201 + Location ; 409 si email ou identifiant déjà utilisé. */
    @PostMapping
    public ResponseEntity<DonneurReponse> inscrire(@Valid @RequestBody InscriptionDonneurRequest requete,
                                                   UriComponentsBuilder uri) {
        DonneurReponse donneur = donneurService.inscrire(requete);
        URI location = uri.path("/api/v1/donneurs/{id}").buildAndExpand(donneur.id()).toUri();
        return ResponseEntity.created(location).body(donneur);
    }

    /** Un donneur ne lit que son propre profil. */
    @GetMapping("/{id}")
    public DonneurReponse profil(@PathVariable Long id, Authentication auth) {
        return donneurService.profil(id, UtilisateurConnecte.id(auth));
    }
}
