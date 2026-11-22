package com.donsang.dons.api.dto;

import java.time.LocalDate;

import com.donsang.dons.domaine.GroupeSanguin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record InscriptionDonneurRequest(
        @NotBlank @Size(max = 100) String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String motDePasse,
        @Size(max = 20) String telephone,
        @NotBlank @Size(max = 20) String identifiantNational,
        @NotNull @Past LocalDate dateNaissance,
        @NotNull @DecimalMin(value = "50.0", message = "le poids minimum pour donner est de 50 kg") Float poids,
        @NotNull GroupeSanguin groupeSanguin) {
}
