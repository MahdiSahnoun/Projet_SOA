package com.donsang.dons.api.dto;

import jakarta.validation.constraints.NotBlank;

/** Le donneur indique la banque où il donne ; son groupe sanguin est déjà connu. */
public record ProposerDonRequest(@NotBlank String codeBanque) {
}
