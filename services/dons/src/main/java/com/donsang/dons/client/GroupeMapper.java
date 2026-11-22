package com.donsang.dons.client;

import com.donsang.dons.domaine.GroupeSanguin;

/** Conversion entre l'énumération du domaine et celle générée depuis le .proto (mêmes noms). */
final class GroupeMapper {

    private GroupeMapper() {
    }

    static com.donsang.inventaire.v1.GroupeSanguin versProto(GroupeSanguin groupe) {
        return com.donsang.inventaire.v1.GroupeSanguin.valueOf(groupe.name());
    }

    static GroupeSanguin versDomaine(com.donsang.inventaire.v1.GroupeSanguin groupe) {
        return GroupeSanguin.valueOf(groupe.name());
    }
}
