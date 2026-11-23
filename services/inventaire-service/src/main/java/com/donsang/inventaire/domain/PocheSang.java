package com.donsang.inventaire.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDate;

@Entity
@Table(name = "poches")
public class PocheSang {
    @Id
    @Column(name = "id", nullable = false)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "banque_id", nullable = false)
    private Banque banque;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GroupeSanguin groupe;

    @Column(name = "date_collecte", nullable = false)
    private LocalDate dateCollecte;

    @Column(name = "date_peremption", nullable = false)
    private LocalDate datePeremption;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatutPoche statut;

    @Column(name = "reference_reservation")
    private String referenceReservation;

    @Version
    private long version;

    protected PocheSang() {}

    public PocheSang(String id, Banque banque, GroupeSanguin groupe,
                     LocalDate dateCollecte, LocalDate datePeremption) {
        this.id = id;
        this.banque = banque;
        this.groupe = groupe;
        this.dateCollecte = dateCollecte;
        this.datePeremption = datePeremption;
        this.statut = StatutPoche.DISPONIBLE;
    }

    public String getId() { return id; }
    public Banque getBanque() { return banque; }
    public GroupeSanguin getGroupe() { return groupe; }
    public LocalDate getDateCollecte() { return dateCollecte; }
    public LocalDate getDatePeremption() { return datePeremption; }
    public StatutPoche getStatut() { return statut; }
    public String getReferenceReservation() { return referenceReservation; }
    public long getVersion() { return version; }

    public void reserver(String referenceReservation) {
        this.statut = StatutPoche.RESERVEE;
        this.referenceReservation = referenceReservation;
    }

    public void liberer() {
        this.statut = StatutPoche.DISPONIBLE;
        this.referenceReservation = null;
    }

    public void confirmerSortie() {
        this.statut = StatutPoche.SORTIE;
    }

    public void setStatut(StatutPoche statut) {
        this.statut = statut;
    }
}
