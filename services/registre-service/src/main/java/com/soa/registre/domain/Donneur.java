package com.soa.registre.domain;

import jakarta.persistence.*;
import java.time.LocalDate;

/**
 * Entité JPA représentant un donneur de sang dans registre_db.
 *
 * Champs définis collectivement (voir document de répartition des tâches).
 */
@Entity
@Table(name = "donneur")
public class Donneur {

    @Id
    private String id;           // ex. DON-001

    private String nom;
    private String prenom;
    private String cin;

    private LocalDate dateNaissance;

    private String sexe;         // M ou F

    @Enumerated(EnumType.STRING)
    private GroupeSanguin groupeSanguin;

    private int poids;           // kg

    private LocalDate dateDernierDon;   // nullable

    private boolean contreIndication;

    private String ville;
    private String telephone;

    // ----------------------------------------------------------------
    // Constructeurs
    // ----------------------------------------------------------------

    public Donneur() {}

    // ----------------------------------------------------------------
    // Getters / Setters
    // ----------------------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getCin() { return cin; }
    public void setCin(String cin) { this.cin = cin; }

    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }

    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }

    public GroupeSanguin getGroupeSanguin() { return groupeSanguin; }
    public void setGroupeSanguin(GroupeSanguin groupeSanguin) { this.groupeSanguin = groupeSanguin; }

    public int getPoids() { return poids; }
    public void setPoids(int poids) { this.poids = poids; }

    public LocalDate getDateDernierDon() { return dateDernierDon; }
    public void setDateDernierDon(LocalDate dateDernierDon) { this.dateDernierDon = dateDernierDon; }

    public boolean isContreIndication() { return contreIndication; }
    public void setContreIndication(boolean contreIndication) { this.contreIndication = contreIndication; }

    public String getVille() { return ville; }
    public void setVille(String ville) { this.ville = ville; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}
