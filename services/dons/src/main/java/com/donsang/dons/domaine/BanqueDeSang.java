package com.donsang.dons.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Compte utilisateur d'une banque de sang.
 * Le stock, lui, vit dans le service Inventaire : ici on garde seulement son code (ex. BANQUE-A).
 */
@Entity
@Table(name = "banque_de_sang")
@Getter
@Setter
public class BanqueDeSang extends Utilisateur {

    @Column(nullable = false)
    private String nomBanque;

    private String adresse;

    /** Identifiant de la banque dans le service Inventaire. */
    @Column(nullable = false, unique = true)
    private String codeBanque;

    @Override
    public Role getRole() {
        return Role.BANQUE;
    }
}
