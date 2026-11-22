package com.soa.registre.service;

import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import com.soa.registre.exception.DonneurInconnuException;
import com.soa.registre.exception.DonneurInelegibleException;
import com.soa.registre.exception.GroupeSanguinInvalideException;
import com.soa.registre.repository.DonneurRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;

/**
 * Logique métier du service Registre. Les règles d'éligibilité ne sont écrites
 * qu'une seule fois, dans {@link #evaluer(Donneur, LocalDate)}.
 *
 * Règles (T08) :
 *   - Âge entre 18 et 65 ans
 *   - Délai depuis le dernier don >= 56 jours (ou jamais donné)
 *   - Poids >= 50 kg
 *   - Pas de contre-indication
 */
@Service
public class RegistreService {

    static final int AGE_MIN = 18;
    static final int AGE_MAX = 65;
    static final int DELAI_MIN_JOURS = 56;
    static final int POIDS_MIN_KG = 50;

    private static final DateTimeFormatter FORMAT_NUMERO = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final DonneurRepository donneurRepository;

    public RegistreService(DonneurRepository donneurRepository) {
        this.donneurRepository = donneurRepository;
    }

    // ----------------------------------------------------------------
    // T04 — VerifierExistence : jamais de Fault
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public boolean verifierExistence(String identifiantNational) {
        return donneurRepository.existsById(identifiantNational);
    }

    // ----------------------------------------------------------------
    // T08 — VerifierEligibilite : Fault seulement si le donneur est inconnu
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public ResultatEligibilite verifierEligibilite(String identifiantNational) {
        return evaluer(trouver(identifiantNational), LocalDate.now());
    }

    // ----------------------------------------------------------------
    // T08 — EnregistrerDon : Fault si inconnu ou inéligible
    // ----------------------------------------------------------------

    /**
     * Vérifie l'éligibilité à la date du don, met à jour dateDernierDon
     * et renvoie un numéro d'enregistrement unique : REG-{identifiant}-{yyyyMMdd}.
     * (Unique : après un don, le donneur est inéligible 56 jours, donc un seul don par jour.)
     */
    @Transactional
    public String enregistrerDon(String identifiantNational, LocalDate dateDon) {
        Donneur donneur = trouver(identifiantNational);

        ResultatEligibilite resultat = evaluer(donneur, dateDon);
        if (!resultat.eligible()) {
            throw new DonneurInelegibleException(identifiantNational, resultat.motif());
        }

        donneur.setDateDernierDon(dateDon);
        donneurRepository.save(donneur);
        return "REG-" + identifiantNational + "-" + dateDon.format(FORMAT_NUMERO);
    }

    // ----------------------------------------------------------------
    // T15 — ListerDonneursEligibles par groupe sanguin
    // ----------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<Donneur> listerDonneursEligibles(GroupeSanguin groupeSanguin) {
        LocalDate aujourdHui = LocalDate.now();
        return donneurRepository.findByGroupeSanguin(groupeSanguin).stream()
                .filter(d -> evaluer(d, aujourdHui).eligible())
                .toList();
    }

    // ----------------------------------------------------------------
    // Conversion du texte "groupe" reçu dans le message SOAP
    // ----------------------------------------------------------------

    public GroupeSanguin groupeSanguin(String valeur) {
        if (valeur != null) {
            for (GroupeSanguin g : GroupeSanguin.values()) {
                if (g.name().equals(valeur.trim())) {
                    return g;
                }
            }
        }
        throw new GroupeSanguinInvalideException(valeur, Arrays.toString(GroupeSanguin.values()));
    }

    // ----------------------------------------------------------------
    // Règles d'éligibilité (un seul endroit)
    // ----------------------------------------------------------------

    /**
     * Évalue toutes les règles à la date de référence, sans lever d'exception.
     * Le motif est celui de la première règle non respectée ; prochainDonPossible
     * n'est renseigné que si le délai entre deux dons n'est pas respecté.
     */
    ResultatEligibilite evaluer(Donneur donneur, LocalDate reference) {
        String motif = null;

        int age = Period.between(donneur.getDateNaissance(), reference).getYears();
        if (age < AGE_MIN || age > AGE_MAX) {
            motif = "âge hors limites : " + age + " ans (attendu " + AGE_MIN + "–" + AGE_MAX + ")";
        }

        LocalDate prochainDonPossible = null;
        if (donneur.getDateDernierDon() != null) {
            long jours = ChronoUnit.DAYS.between(donneur.getDateDernierDon(), reference);
            if (jours < DELAI_MIN_JOURS) {
                prochainDonPossible = donneur.getDateDernierDon().plusDays(DELAI_MIN_JOURS);
                if (motif == null) {
                    motif = "délai insuffisant depuis le dernier don : " + jours
                            + " jours (minimum " + DELAI_MIN_JOURS + ")";
                }
            }
        }

        if (motif == null && donneur.getPoids() < POIDS_MIN_KG) {
            motif = "poids insuffisant : " + donneur.getPoids() + " kg (minimum " + POIDS_MIN_KG + ")";
        }

        if (motif == null && donneur.isContreIndication()) {
            motif = "contre-indication médicale enregistrée";
        }

        return new ResultatEligibilite(motif == null, motif, prochainDonPossible);
    }

    private Donneur trouver(String identifiantNational) {
        return donneurRepository.findById(identifiantNational)
                .orElseThrow(() -> new DonneurInconnuException(identifiantNational));
    }
}
