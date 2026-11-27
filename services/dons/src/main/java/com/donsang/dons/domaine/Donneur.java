package com.donsang.dons.domaine;

import java.time.LocalDate;
import java.time.Period;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "donneur")
@Getter
@Setter
public class Donneur extends Utilisateur {

    /** Clé de rapprochement avec le registre national (SOAP). */
    @Column(nullable = false, unique = true)
    private String identifiantNational;

    @Column(nullable = false)
    private LocalDate dateNaissance;

    @Column(nullable = false)
    private float poids;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupeSanguin groupeSanguin;

    private LocalDate dateDernierDon;

    @Column(nullable = false, columnDefinition = "boolean default false")
    private boolean accepteAlertesDon;

    @Override
    public Role getRole() {
        return Role.DONNEUR;
    }

    public boolean estMajeur(LocalDate aujourdhui) {
        return Period.between(dateNaissance, aujourdhui).getYears() >= 18;
    }

    public boolean estMajeur() {
        return estMajeur(LocalDate.now());
    }
}
