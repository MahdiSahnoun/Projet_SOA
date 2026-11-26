package com.donsang.dons.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donsang.dons.api.dto.DonneurReponse;
import com.donsang.dons.api.dto.InscriptionDonneurRequest;
import com.donsang.dons.client.RegistreClient;
import com.donsang.dons.depot.DonneurRepository;
import com.donsang.dons.depot.UtilisateurRepository;
import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.erreur.ConflitException;
import com.donsang.dons.erreur.RequeteInvalideException;
import com.donsang.dons.erreur.RessourceIntrouvableException;
import com.donsang.dons.notification.EmailNotificationPublisher;
import com.donsang.dons.notification.EmailNotificationEvent;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class DonneurService {

    private final UtilisateurRepository utilisateurs;
    private final DonneurRepository donneurs;
    private final PasswordEncoder passwordEncoder;
    private final RegistreClient registre;
    private final Clock clock;
    private final EmailNotificationPublisher notifications;

    public DonneurReponse inscrire(InscriptionDonneurRequest r) {
        String email = r.email().toLowerCase(Locale.ROOT);
        if (utilisateurs.existsByEmail(email)) {
            throw new ConflitException("Un compte existe déjà avec cet email");
        }
        if (donneurs.existsByIdentifiantNational(r.identifiantNational())) {
            throw new ConflitException("Ce donneur est déjà inscrit");
        }

        Donneur donneur = new Donneur();
        donneur.setNom(r.nom());
        donneur.setEmail(email);
        donneur.setMotDePasse(passwordEncoder.encode(r.motDePasse()));
        donneur.setTelephone(r.telephone());
        donneur.setIdentifiantNational(r.identifiantNational());
        donneur.setDateNaissance(r.dateNaissance());
        donneur.setPoids(r.poids());
        donneur.setGroupeSanguin(r.groupeSanguin());
        donneur.setAccepteAlertesDon(r.accepteAlertesDon());

        if (!donneur.estMajeur(LocalDate.now(clock))) {
            throw new RequeteInvalideException("Le donneur doit être majeur");
        }
        // Appel SOAP VerifierExistence (repli : accepté si le Registre est injoignable)............
        if (!registre.existe(r.identifiantNational())) {
            throw new RequeteInvalideException("Identifiant inconnu du registre national");
        }
        Donneur enregistre = donneurs.save(donneur);
        notifications.publierApresCommit(new EmailNotificationEvent(
                null,
                EmailNotificationEvent.Type.DONNEUR_INSCRIT,
                enregistre.getEmail(),
                enregistre.getNom(),
                "DONNEUR-" + enregistre.getId(),
                java.util.Map.of("groupeSanguin", enregistre.getGroupeSanguin().name())));
        return DonneurReponse.de(enregistre);
    }

    @Transactional(readOnly = true)
    public DonneurReponse profil(Long id, Long idConnecte) {
        if (!id.equals(idConnecte)) {
            // 404 plutôt que 403 : on ne révèle pas l'existence des autres comptes
            throw new RessourceIntrouvableException("Donneur " + id + " introuvable");
        }
        return donneurs.findById(id).map(DonneurReponse::de)
                .orElseThrow(() -> new RessourceIntrouvableException("Donneur " + id + " introuvable"));
    }

    public void modifierPreferenceAlertes(Long id, Long idConnecte, boolean accepter) {
        if (!id.equals(idConnecte)) {
            throw new RessourceIntrouvableException("Donneur " + id + " introuvable");
        }
        Donneur donneur = donneurs.findById(id)
                .orElseThrow(() -> new RessourceIntrouvableException("Donneur " + id + " introuvable"));
        donneur.setAccepteAlertesDon(accepter);
    }
}
