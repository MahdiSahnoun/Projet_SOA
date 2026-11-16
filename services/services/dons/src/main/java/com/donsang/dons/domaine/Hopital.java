package com.donsang.dons.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "hopital")
@Getter
@Setter
public class Hopital extends Utilisateur {

    @Column(nullable = false)
    private String nomEtablissement;

    private String adresse;

    @Override
    public Role getRole() {
        return Role.HOPITAL;
    }
}
