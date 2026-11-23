package com.donsang.inventaire.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "banques")
public class Banque {
    @Id
    @Column(name = "banque_id", nullable = false)
    private String banqueId;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String ville;

    protected Banque() {}

    public Banque(String banqueId, String nom, String ville) {
        this.banqueId = banqueId;
        this.nom = nom;
        this.ville = ville;
    }

    public String getBanqueId() { return banqueId; }
    public String getNom() { return nom; }
    public String getVille() { return ville; }
}
