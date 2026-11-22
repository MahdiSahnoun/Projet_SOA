package com.soa.registre.service;

import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import com.soa.registre.exception.DonneurInconnuException;
import com.soa.registre.exception.DonneurInelegibleException;
import com.soa.registre.repository.DonneurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Logique métier du service Registre.
 *
 * Règles d'éligibilité (T08) :
 *   - Âge entre 18 et 65 ans
 *   - Délai depuis le dernier don >= 56 jours (ou jamais donné)
 *   - Poids >= 50 kg
 *   - Pas de contre-indication
 */
@Service
public class RegistreService {

    private static final int AGE_MIN = 18;
    private static final int AGE_MAX = 65;
    private static final int DELAI_MIN_JOURS = 56;
    private static final int POIDS_MIN_KG = 50;

    private final DonneurRepository donneurRepository;

    public RegistreService(DonneurRepository donneurRepository) {
        this.donneurRepository = donneurRepository;
    }

    // ----------------------------------------------------------------
    // T04 — VerifierExistence
    // ----------------------------------------------------------------

    public boolean verifierExistence(String donneurId) {
        return donneurRepository.existsById(donneurId);
    }

    // ----------------------------------------------------------------
    // T08 — VerifierEligibilite
    // Lève DonneurInconnuException ou DonneurInelegibleException si KO.
    // ----------------------------------------------------------------

    public void verifierEligibilite(String donneurId) {
        Donneur donneur = donneurRepository.findById(donneurId)
                .orElseThrow(() -> new DonneurInconnuException(donneurId));

        validerEligibilite(donneur);
    }

    // ----------------------------------------------------------------
    // T08 — EnregistrerDon
    // Vérifie l'éligibilité puis met à jour dateDernierDon.
    // ----------------------------------------------------------------

    @Transactional
    public void enregistrerDon(String donneurId) {
        Donneur donneur = donneurRepository.findById(donneurId)
                .orElseThrow(() -> new DonneurInconnuException(donneurId));

        validerEligibilite(donneur);

        donneur.setDateDernierDon(LocalDate.now());
        donneurRepository.save(donneur);
    }

    // ----------------------------------------------------------------
    // T15 — ListerDonneursEligibles par groupe sanguin
    // ----------------------------------------------------------------

    public List<Donneur> listerDonneursEligibles(GroupeSanguin groupeSanguin) {
        return donneurRepository.findByGroupeSanguin(groupeSanguin).stream()
                .filter(this::estEligible)
                .toList();
    }

    // ----------------------------------------------------------------
    // Méthodes internes
    // ----------------------------------------------------------------

    /**
     * Applique toutes les règles d'éligibilité et lève une exception si une règle n'est pas respectée.
     */
    private void validerEligibilite(Donneur donneur) {
        LocalDate aujourd_hui = LocalDate.now();

        // Âge
        int age = Period.between(donneur.getDateNaissance(), aujourd_hui).getYears();
        if (age < AGE_MIN || age > AGE_MAX) {
            throw new DonneurInelegibleException(donneur.getId(),
                    "âge hors limites : " + age + " ans (attendu 18–65)");
        }

        // Délai depuis le dernier don
        if (donneur.getDateDernierDon() != null) {
            long jours = donneur.getDateDernierDon().until(aujourd_hui).getDays();
            // Period.getDays() ne retourne pas le total — utiliser ChronoUnit
            long totalJours = java.time.temporal.ChronoUnit.DAYS.between(
                    donneur.getDateDernierDon(), aujourd_hui);
            if (totalJours < DELAI_MIN_JOURS) {
                throw new DonneurInelegibleException(donneur.getId(),
                        "délai insuffisant depuis le dernier don : " + totalJours + " jours (minimum " + DELAI_MIN_JOURS + ")");
            }
        }

        // Poids
        if (donneur.getPoids() < POIDS_MIN_KG) {
            throw new DonneurInelegibleException(donneur.getId(),
                    "poids insuffisant : " + donneur.getPoids() + " kg (minimum " + POIDS_MIN_KG + ")");
        }

        // Contre-indication
        if (donneur.isContreIndication()) {
            throw new DonneurInelegibleException(donneur.getId(),
                    "contre-indication médicale enregistrée");
        }
    }

    /**
     * Version booléenne pour le filtrage de liste (T15) — ne lève pas d'exception.
     */
    private boolean estEligible(Donneur donneur) {
        try {
            validerEligibilite(donneur);
            return true;
        } catch (DonneurInelegibleException e) {
            return false;
        }
    }
}
