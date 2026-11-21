package com.donsang.dons.api.dto;

import com.donsang.dons.domaine.GroupeSanguin;

/** codeBanque est null quand le stock porte sur toutes les banques. */
public record StockReponse(GroupeSanguin groupeSanguin, String codeBanque, int disponibles) {
}
