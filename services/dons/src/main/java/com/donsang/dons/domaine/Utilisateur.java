package com.donsang.dons.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/** Classe abstraite du diagramme : une table commune + une table par sous-classe (JOINED). */
@Entity
@Table(name = "utilisateur")
@Inheritance(strategy = InheritanceType.JOINED)
@Getter
@Setter
public abstract class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false, unique = true)
    private String email;

    /** Haché en BCrypt, jamais stocké en clair. */
    @Column(nullable = false)
    private String motDePasse;

    private String telephone;

    /** Rôle placé dans le jeton JWT. */
    public abstract Role getRole();
}
