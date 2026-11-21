package com.donsang.dons.api.dto;

import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.NiveauUrgence;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreerDemandeRequest(
        @NotNull GroupeSanguin groupeSanguin,
        @Min(1) @Max(20) int quantite,
        @NotNull NiveauUrgence urgence) {
}
