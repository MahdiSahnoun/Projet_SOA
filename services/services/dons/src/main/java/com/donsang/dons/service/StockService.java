package com.donsang.dons.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donsang.dons.api.dto.EnregistrerPocheRequest;
import com.donsang.dons.api.dto.PocheReponse;
import com.donsang.dons.api.dto.StockReponse;
import com.donsang.dons.client.InventaireClient;
import com.donsang.dons.depot.BanqueDeSangRepository;
import com.donsang.dons.domaine.BanqueDeSang;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.erreur.RequeteInvalideException;
import com.donsang.dons.erreur.RessourceIntrouvableException;

import lombok.RequiredArgsConstructor;

/** Relais REST vers Inventaire pour la consultation du stock et l'entrée de poches par une banque. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final InventaireClient inventaire;
    private final BanqueDeSangRepository banques;

    public StockReponse stock(GroupeSanguin groupe, String codeBanque) {
        return new StockReponse(groupe, codeBanque, inventaire.stock(groupe, codeBanque));
    }

    public PocheReponse enregistrerPoche(Long banqueId, EnregistrerPocheRequest requete) {
        BanqueDeSang banque = banques.findById(banqueId)
                .orElseThrow(() -> new RessourceIntrouvableException("Banque " + banqueId + " introuvable"));
        if (!requete.datePeremption().isAfter(requete.dateCollecte())) {
            throw new RequeteInvalideException("La date de péremption doit être postérieure à la date de collecte");
        }
        String pocheId = inventaire.ajouterPoche(banque.getCodeBanque(), requete.groupeSanguin(),
                requete.volumeMl(), requete.dateCollecte(), requete.datePeremption());
        return new PocheReponse(pocheId, banque.getCodeBanque());
    }
}
