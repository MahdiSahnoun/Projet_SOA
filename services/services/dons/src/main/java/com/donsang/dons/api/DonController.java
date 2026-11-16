package com.donsang.dons.api;

import java.net.URI;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.donsang.dons.api.dto.DonReponse;
import com.donsang.dons.api.dto.ProposerDonRequest;
import com.donsang.dons.domaine.StatutServiceDon;
import com.donsang.dons.securite.UtilisateurConnecte;
import com.donsang.dons.service.ServiceDon;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/dons")
@RequiredArgsConstructor
@Tag(name = "Dons")
public class DonController {

    private final ServiceDon serviceDon;

    /**
     * 201 Created : don accepté ou refusé (décision prise).
     * 202 Accepted : décision différée (Registre indisponible, méthode de repli).
     */
    @PostMapping
    public ResponseEntity<DonReponse> proposer(@Valid @RequestBody ProposerDonRequest requete,
                                               Authentication auth, UriComponentsBuilder uri) {
        DonReponse don = serviceDon.traiterDon(UtilisateurConnecte.id(auth), requete);
        URI location = uri.path("/api/v1/dons/{id}").buildAndExpand(don.id()).toUri();
        HttpStatus statut = don.statut() == StatutServiceDon.EN_ATTENTE ? HttpStatus.ACCEPTED : HttpStatus.CREATED;
        return ResponseEntity.status(statut).location(location).body(don);
    }

    @GetMapping
    public List<DonReponse> mesDons(Authentication auth) {
        return serviceDon.mesDons(UtilisateurConnecte.id(auth));
    }

    @GetMapping("/{id}")
    public DonReponse monDon(@PathVariable Long id, Authentication auth) {
        return serviceDon.monDon(id, UtilisateurConnecte.id(auth));
    }
}
