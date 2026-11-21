package com.donsang.dons.api.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.donsang.dons.domaine.Demande;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.NiveauUrgence;
import com.donsang.dons.domaine.StatutServiceDon;

public record DemandeReponse(Long id, GroupeSanguin groupeSanguin, int quantite, NiveauUrgence urgence,
                             StatutServiceDon statut, LocalDateTime dateDemande,
                             List<AllocationReponse> allocations) {

    public static DemandeReponse de(Demande d) {
        return new DemandeReponse(d.getId(), d.getGroupeSanguin(), d.getQuantite(), d.getUrgence(),
                d.getStatut(), d.getDateDemande(),
                d.getAllocations().stream().map(AllocationReponse::de).toList());
    }
}
