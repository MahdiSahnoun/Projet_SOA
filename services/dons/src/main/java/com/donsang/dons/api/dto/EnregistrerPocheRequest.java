package com.donsang.dons.api.dto;

import java.time.LocalDate;

import com.donsang.dons.domaine.GroupeSanguin;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;

public record EnregistrerPocheRequest(
        @NotNull GroupeSanguin groupeSanguin,
        @Min(200) @Max(600) int volumeMl,
        @NotNull @PastOrPresent LocalDate dateCollecte,
        @NotNull @Future LocalDate datePeremption) {
}
