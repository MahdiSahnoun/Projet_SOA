package com.donsang.dons.api.dto;

import com.donsang.dons.domaine.Allocation;
import com.donsang.dons.domaine.GroupeSanguin;

public record AllocationReponse(String codeBanque, GroupeSanguin groupeSanguin, int quantite) {

    public static AllocationReponse de(Allocation a) {
        return new AllocationReponse(a.getCodeBanque(), a.getGroupeSanguin(), a.getQuantite());
    }
}
