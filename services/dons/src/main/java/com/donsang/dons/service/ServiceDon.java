package com.donsang.dons.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.donsang.dons.api.dto.CreerDemandeRequest;
import com.donsang.dons.api.dto.DemandeReponse;
import com.donsang.dons.api.dto.DonReponse;
import com.donsang.dons.api.dto.ProposerDonRequest;
import com.donsang.dons.client.Eligibilite;
import com.donsang.dons.client.InventaireClient;
import com.donsang.dons.client.RegistreClient;
import com.donsang.dons.depot.BanqueDeSangRepository;
import com.donsang.dons.depot.DemandeRepository;
import com.donsang.dons.depot.DonRepository;
import com.donsang.dons.depot.DonneurRepository;
import com.donsang.dons.depot.HopitalRepository;
import com.donsang.dons.domaine.Demande;
import com.donsang.dons.domaine.Don;
import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.Hopital;
import com.donsang.dons.domaine.StatutServiceDon;
import com.donsang.dons.erreur.InventaireIndisponibleException;
import com.donsang.dons.erreur.RegistreIndisponibleException;
import com.donsang.dons.erreur.RessourceIntrouvableException;
import com.donsang.dons.erreur.StockInsuffisantException;

import lombok.RequiredArgsConstructor;

/**
 * Orchestrateur du diagramme de classes (ServiceDon) :
 * traiterDon (REST -> SOAP -> gRPC) et traiterDemande (REST -> gRPC).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ServiceDon {

    static final int VOLUME_STANDARD_ML = 450;
    static final int DUREE_CONSERVATION_JOURS = 42;

    private final DonneurRepository donneurs;
    private final HopitalRepository hopitaux;
    private final BanqueDeSangRepository banques;
    private final DonRepository dons;
    private final DemandeRepository demandes;
    private final RegistreClient registre;
    private final InventaireClient inventaire;
    private final Clock clock;

    // ================================================================ Dons

    /**
     * 1. Contrôles locaux (banque connue, majorité).
     * 2. SOAP VerifierEligibilite : éligible, refusé, ou « en attente » si le Registre est en panne.
     * 3. Si éligible : SOAP EnregistrerDon puis gRPC AjouterPoche.
     */
    public DonReponse traiterDon(Long donneurId, ProposerDonRequest requete) {
        Donneur donneur = donneurs.findById(donneurId)
                .orElseThrow(() -> new RessourceIntrouvableException("Donneur " + donneurId + " introuvable"));
        if (!banques.existsByCodeBanque(requete.codeBanque())) {
            throw new RessourceIntrouvableException("Banque " + requete.codeBanque() + " inconnue");
        }
        LocalDate aujourdhui = LocalDate.now(clock);
        Don don = dons.save(new Don(donneur, requete.codeBanque(), LocalDateTime.now(clock)));

        if (!donneur.estMajeur(aujourdhui)) {
            don.refuser("Le donneur doit être majeur");
            return DonReponse.de(don);
        }

        Eligibilite eligibilite = registre.verifier(donneur.getIdentifiantNational());
        switch (eligibilite.etat()) {
            case INELIGIBLE -> don.refuser(eligibilite.motif());
            case EN_ATTENTE -> don.mettreEnAttente(eligibilite.motif());
            case ELIGIBLE -> accepter(don, donneur, aujourdhui);
        }
        return DonReponse.de(don);
    }

    private void accepter(Don don, Donneur donneur, LocalDate aujourdhui) {
        String numero;
        try {
            numero = registre.enregistrerDon(donneur.getIdentifiantNational(), aujourdhui, donneur.getGroupeSanguin());
        } catch (RegistreIndisponibleException e) {
            don.mettreEnAttente("Registre indisponible : enregistrement officiel différé");
            return;
        }
        donneur.setDateDernierDon(aujourdhui);
        try {
            inventaire.ajouterPoche(don.getCodeBanque(), don.getGroupeSanguin(), VOLUME_STANDARD_ML,
                    aujourdhui, aujourdhui.plusDays(DUREE_CONSERVATION_JOURS));
            don.accepter(numero, null);
        } catch (InventaireIndisponibleException e) {
            // Le don est officiellement enregistré ; la poche devra être saisie par la banque.
            don.accepter(numero, "Inventaire indisponible : poche à saisir par la banque");
        }
    }

    @Transactional(readOnly = true)
    public List<DonReponse> mesDons(Long donneurId) {
        return dons.findByDonneurIdOrderByDateServiceDesc(donneurId).stream().map(DonReponse::de).toList();
    }

    @Transactional(readOnly = true)
    public DonReponse monDon(Long id, Long donneurId) {
        return dons.findByIdAndDonneurId(id, donneurId).map(DonReponse::de)
                .orElseThrow(() -> new RessourceIntrouvableException("Don " + id + " introuvable"));
    }

    // ================================================================ Demandes

    /**
     * Calcule les groupes compatibles, demande à Inventaire de réserver (une ou plusieurs banques)
     * et garde la répartition. Stock insuffisant ou Inventaire en panne : demande EN_ATTENTE.
     */
    public DemandeReponse traiterDemande(Long hopitalId, CreerDemandeRequest requete) {
        Hopital hopital = hopitaux.findById(hopitalId)
                .orElseThrow(() -> new RessourceIntrouvableException("Hôpital " + hopitalId + " introuvable"));
        Demande demande = demandes.save(new Demande(hopital, requete.groupeSanguin(), requete.quantite(),
                requete.urgence(), LocalDateTime.now(clock)));
        try {
            inventaire.reserver(groupesCompatibles(requete.groupeSanguin()), requete.quantite(), demande.reference())
                    .forEach(a -> demande.ajouterAllocation(a.codeBanque(), a.groupeSanguin(), a.quantite()));
            demande.accepter();
        } catch (StockInsuffisantException | InventaireIndisponibleException e) {
            demande.mettreEnAttente();
        }
        return DemandeReponse.de(demande);
    }

    @Transactional(readOnly = true)
    public Page<DemandeReponse> mesDemandes(Long hopitalId, Pageable pageable) {
        return demandes.findByHopitalId(hopitalId, pageable).map(DemandeReponse::de);
    }

    @Transactional(readOnly = true)
    public DemandeReponse maDemande(Long id, Long hopitalId) {
        return DemandeReponse.de(demandeDe(id, hopitalId));
    }

    /** Libère les poches réservées puis annule. 409 si la demande est déjà annulée. */
    public DemandeReponse annulerDemande(Long id, Long hopitalId) {
        Demande demande = demandeDe(id, hopitalId);
        if (demande.getStatut() == StatutServiceDon.ACCEPTEE) {
            inventaire.liberer(demande.reference());
        }
        demande.annuler();
        return DemandeReponse.de(demande);
    }

    public List<GroupeSanguin> groupesCompatibles(GroupeSanguin groupe) {
        return Compatibilite.groupesCompatibles(groupe);
    }

    private Demande demandeDe(Long id, Long hopitalId) {
        return demandes.findByIdAndHopitalId(id, hopitalId)
                .orElseThrow(() -> new RessourceIntrouvableException("Demande " + id + " introuvable"));
    }
}
