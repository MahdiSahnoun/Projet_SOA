package com.donsang.dons.api;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import com.donsang.dons.api.dto.CreerDemandeRequest;
import com.donsang.dons.api.dto.DemandeReponse;
import com.donsang.dons.securite.UtilisateurConnecte;
import com.donsang.dons.service.ServiceDon;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/demandes")
@RequiredArgsConstructor
@Tag(name = "Demandes hospitalières")
public class DemandeController {

    private final ServiceDon serviceDon;

    /** 201 + Location. Le statut dit si les poches sont réservées (ACCEPTEE) ou non (EN_ATTENTE). */
    @PostMapping
    public ResponseEntity<DemandeReponse> creer(@Valid @RequestBody CreerDemandeRequest requete,
                                                Authentication auth, UriComponentsBuilder uri) {
        DemandeReponse demande = serviceDon.traiterDemande(UtilisateurConnecte.id(auth), requete);
        URI location = uri.path("/api/v1/demandes/{id}").buildAndExpand(demande.id()).toUri();
        return ResponseEntity.created(location).body(demande);
    }

    /** Historique paginé : ?page=0&size=20&sort=dateDemande,desc */
    @GetMapping
    public Page<DemandeReponse> mesDemandes(Authentication auth,
            @PageableDefault(size = 20, sort = "dateDemande", direction = Sort.Direction.DESC) Pageable pageable) {
        return serviceDon.mesDemandes(UtilisateurConnecte.id(auth), pageable);
    }

    @GetMapping("/{id}")
    public DemandeReponse suivre(@PathVariable Long id, Authentication auth) {
        return serviceDon.maDemande(id, UtilisateurConnecte.id(auth));
    }

    /** 200 si annulée ; 404 si absente ; 409 si déjà annulée. */
    @PatchMapping("/{id}/annulation")
    public DemandeReponse annuler(@PathVariable Long id, Authentication auth) {
        return serviceDon.annulerDemande(id, UtilisateurConnecte.id(auth));
    }
}
