package com.donsang.dons.domaine;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.donsang.dons.erreur.ConflitException;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Demande de poches émise par un hôpital. */
@Entity
@Table(name = "demande")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Demande {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "hopital_id", nullable = false)
    private Hopital hopital;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupeSanguin groupeSanguin;

    @Column(nullable = false)
    private int quantite;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NiveauUrgence urgence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutServiceDon statut;

    @Column(nullable = false)
    private LocalDateTime dateDemande;

    /** Répartition renvoyée par Inventaire (ex. 1 poche banque A + 2 banque B). */
    @OneToMany(mappedBy = "demande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Allocation> allocations = new ArrayList<>();

    public Demande(Hopital hopital, GroupeSanguin groupeSanguin, int quantite,
                   NiveauUrgence urgence, LocalDateTime dateDemande) {
        this.hopital = hopital;
        this.groupeSanguin = groupeSanguin;
        this.quantite = quantite;
        this.urgence = urgence;
        this.dateDemande = dateDemande;
        this.statut = StatutServiceDon.EN_ATTENTE;
    }

    /** Référence opaque transmise à Inventaire : il ne sait pas qu'un hôpital est derrière. */
    public String reference() {
        return "DEM-" + id;
    }

    public void ajouterAllocation(String codeBanque, GroupeSanguin groupe, int nombre) {
        allocations.add(new Allocation(this, codeBanque, groupe, nombre));
    }

    public void accepter() {
        this.statut = StatutServiceDon.ACCEPTEE;
    }

    public void mettreEnAttente() {
        this.statut = StatutServiceDon.EN_ATTENTE;
    }

    public void annuler() {
        if (statut == StatutServiceDon.ANNULEE || statut == StatutServiceDon.REFUSEE) {
            throw new ConflitException("La demande " + id + " est déjà au statut " + statut);
        }
        this.statut = StatutServiceDon.ANNULEE;
    }

    public List<Allocation> getAllocations() {
        return Collections.unmodifiableList(allocations);
    }
}
