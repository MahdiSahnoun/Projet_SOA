package com.donsang.dons.domaine;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Trace locale de la répartition : Dons ne lit jamais la base d'Inventaire. */
@Entity
@Table(name = "allocation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Allocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "demande_id", nullable = false)
    private Demande demande;

    /** Référence vers une banque d'Inventaire : un identifiant, pas une clé étrangère. */
    @Column(nullable = false)
    private String codeBanque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupeSanguin groupeSanguin;

    @Column(nullable = false)
    private int quantite;

    Allocation(Demande demande, String codeBanque, GroupeSanguin groupeSanguin, int quantite) {
        this.demande = demande;
        this.codeBanque = codeBanque;
        this.groupeSanguin = groupeSanguin;
        this.quantite = quantite;
    }
}
