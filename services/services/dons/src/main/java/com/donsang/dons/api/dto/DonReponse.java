package com.donsang.dons.api.dto;

import java.time.LocalDateTime;

import com.donsang.dons.domaine.Don;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.StatutServiceDon;

public record DonReponse(Long id, StatutServiceDon statut, GroupeSanguin groupeSanguin, String codeBanque,
                         LocalDateTime dateService, String motif, String numeroEnregistrement) {

    public static DonReponse de(Don d) {
        return new DonReponse(d.getId(), d.getStatut(), d.getGroupeSanguin(), d.getCodeBanque(),
                d.getDateService(), d.getMotif(), d.getNumeroEnregistrement());
    }
}
