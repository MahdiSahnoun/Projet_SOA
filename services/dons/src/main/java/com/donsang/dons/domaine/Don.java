package com.donsang.dons.domaine;

import java.time.LocalDateTime;

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

/** Proposition de don faite par un donneur. */
@Entity
@Table(name = "don")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Don {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "donneur_id", nullable = false)
    private Donneur donneur;

    @Column(nullable = false)
    private String codeBanque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupeSanguin groupeSanguin;

    @Column(nullable = false)
    private LocalDateTime dateService;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutServiceDon statut;

    private String motif;

    /** Numéro attribué par le registre national. */
    private String numeroEnregistrement;

    public Don(Donneur donneur, String codeBanque, LocalDateTime dateService) {
        this.donneur = donneur;
        this.codeBanque = codeBanque;
        this.groupeSanguin = donneur.getGroupeSanguin();
        this.dateService = dateService;
        this.statut = StatutServiceDon.EN_ATTENTE;
    }

    public void accepter(String numeroEnregistrement, String remarque) {
        this.statut = StatutServiceDon.ACCEPTEE;
        this.numeroEnregistrement = numeroEnregistrement;
        this.motif = remarque;
    }

    public void refuser(String motif) {
        this.statut = StatutServiceDon.REFUSEE;
        this.motif = motif;
    }

    public void mettreEnAttente(String motif) {
        this.statut = StatutServiceDon.EN_ATTENTE;
        this.motif = motif;
    }
}
