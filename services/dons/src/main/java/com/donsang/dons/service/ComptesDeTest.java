package com.donsang.dons.service;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.donsang.dons.depot.UtilisateurRepository;
import com.donsang.dons.domaine.BanqueDeSang;
import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.Hopital;
import com.donsang.dons.domaine.Utilisateur;

/**
 * Crée les comptes de démonstration au démarrage, avec le mot de passe COMPTES_TEST_MDP.
 * Aucun mot de passe n'est écrit dans le dépôt. Variable vide : rien n'est créé.
 */
@Component
@Profile("!test")
public class ComptesDeTest implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ComptesDeTest.class);

    private final UtilisateurRepository utilisateurs;
    private final PasswordEncoder passwordEncoder;
    private final String motDePasse;

    public ComptesDeTest(UtilisateurRepository utilisateurs, PasswordEncoder passwordEncoder,
                         @Value("${comptes-test.mot-de-passe:}") String motDePasse) {
        this.utilisateurs = utilisateurs;
        this.passwordEncoder = passwordEncoder;
        this.motDePasse = motDePasse;
    }

    @Override
    public void run(String... args) {
        if (motDePasse == null || motDePasse.isBlank()) {
            LOG.info("COMPTES_TEST_MDP vide : aucun compte de démonstration créé");
            return;
        }
        creerSiAbsent("donneur@test.tn", () -> {
            Donneur d = new Donneur();
            d.setNom("Donneur Démo");
            d.setIdentifiantNational("TN-00000001");   // doit exister dans le registre simulé
            d.setDateNaissance(LocalDate.of(1995, 3, 10));
            d.setPoids(72f);
            d.setGroupeSanguin(GroupeSanguin.O_NEGATIF);
            return d;
        });
        creerSiAbsent("hopital@test.tn", () -> {
            Hopital h = new Hopital();
            h.setNom("Hôpital Démo");
            h.setNomEtablissement("CHU de démonstration");
            h.setAdresse("Tunis");
            return h;
        });
        creerSiAbsent("banque-a@test.tn", () -> banque("Banque A", "BANQUE-A"));
        creerSiAbsent("banque-b@test.tn", () -> banque("Banque B", "BANQUE-B"));
    }

    private BanqueDeSang banque(String nom, String code) {
        BanqueDeSang b = new BanqueDeSang();
        b.setNom(nom);
        b.setNomBanque(nom);
        b.setCodeBanque(code);   // doit correspondre à une banque du service Inventaire
        return b;
    }

    private void creerSiAbsent(String email, Supplier<Utilisateur> fabrique) {
        if (utilisateurs.existsByEmail(email)) {
            return;
        }
        Utilisateur u = fabrique.get();
        u.setEmail(email);
        u.setMotDePasse(passwordEncoder.encode(motDePasse));
        utilisateurs.save(u);
        LOG.info("Compte de démonstration créé : {} ({})", email, u.getRole());
    }
}
