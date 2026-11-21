package com.donsang.dons.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.donsang.dons.api.dto.EnregistrerPocheRequest;
import com.donsang.dons.api.dto.PocheReponse;
import com.donsang.dons.securite.UtilisateurConnecte;
import com.donsang.dons.service.StockService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/poches")
@RequiredArgsConstructor
@Tag(name = "Poches (banques de sang)")
public class PocheController {

    private final StockService stockService;

    /** Une banque enregistre une poche collectée ; elle est ajoutée à son stock dans Inventaire. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PocheReponse enregistrer(@Valid @RequestBody EnregistrerPocheRequest requete, Authentication auth) {
        return stockService.enregistrerPoche(UtilisateurConnecte.id(auth), requete);
    }
}
