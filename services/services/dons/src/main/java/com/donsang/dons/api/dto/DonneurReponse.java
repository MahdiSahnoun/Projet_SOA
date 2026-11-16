package com.donsang.dons.api.dto;

import java.time.LocalDate;

import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.domaine.GroupeSanguin;

/** Jamais de mot de passe dans une réponse. */
public record DonneurReponse(Long id, String nom, String email, GroupeSanguin groupeSanguin,
                             LocalDate dateDernierDon) {

    public static DonneurReponse de(Donneur d) {
        return new DonneurReponse(d.getId(), d.getNom(), d.getEmail(), d.getGroupeSanguin(), d.getDateDernierDon());
    }
}
