package com.donsang.inventaire.service;

import com.donsang.inventaire.domain.Banque;
import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.domain.StatutPoche;
import com.donsang.inventaire.repository.BanqueRepository;
import com.donsang.inventaire.repository.PocheSangRepository;
import com.donsang.inventaire.repartition.CalculateurRepartition;
import com.donsang.inventaire.repartition.LigneRepartition;
import com.donsang.inventaire.repartition.PocheDisponible;
import com.donsang.inventaire.repartition.Repartition;
import com.donsang.inventaire.service.exceptions.AucunePocheReserveeException;
import com.donsang.inventaire.service.exceptions.BanqueInconnueException;
import com.donsang.inventaire.service.exceptions.GroupeInvalideException;
import com.donsang.inventaire.service.exceptions.PocheExistanteException;
import com.donsang.inventaire.service.exceptions.PochePerimeeException;
import com.donsang.inventaire.service.exceptions.ReferenceInconnueException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InventaireService {
    private static final List<String> ORDRE_BANQUES = List.of("BANQUE-A", "BANQUE-B", "BANQUE-C");

    private final BanqueRepository banques;
    private final PocheSangRepository poches;
    private final RegleDePeremption regleDePeremption;
    private final CalculateurRepartition calculateurRepartition;
    private final ApplicationEventPublisher eventPublisher;

    public InventaireService(BanqueRepository banques, PocheSangRepository poches,
                             RegleDePeremption regleDePeremption,
                             ApplicationEventPublisher eventPublisher) {
        this.banques = banques;
        this.poches = poches;
        this.regleDePeremption = regleDePeremption;
        this.calculateurRepartition = new CalculateurRepartition();
        this.eventPublisher = eventPublisher;
    }

    @Transactional(readOnly = true)
    public int getStock(GroupeSanguin groupe, String banqueId) {
        if (banqueId != null && !banqueId.isBlank()) {
            verifierBanque(banqueId);
        }
        String banqueFiltre = banqueId == null || banqueId.isBlank() ? null : banqueId;
        String groupeFiltre = groupe == null ? null : groupe.name();
        return poches.agregerStockDisponible(banqueFiltre, groupeFiltre).stream()
                .mapToInt(agregat -> (int) agregat.getDisponibles())
                .sum();
    }

    @Transactional
    public List<Allocation> reserver(List<GroupeSanguin> groupes, int quantite, String referenceDemande) {
        return reserverPoches(referenceDemande, groupes, quantite);
    }

    @Transactional
    public List<Allocation> reserverPoches(String referenceDemande,
                                           List<GroupeSanguin> groupes,
                                           int quantite) {
        if (quantite <= 0) {
            throw new IllegalArgumentException("La quantite doit etre strictement positive");
        }
        if (referenceDemande == null || referenceDemande.isBlank()) {
            throw new IllegalArgumentException("reference_demande est obligatoire");
        }
        if (groupes == null || groupes.isEmpty()) {
            throw new IllegalArgumentException("Au moins un groupe compatible est obligatoire");
        }

        List<PocheSang> dejaReservees = poches.findByStatutAndReferenceReservation(
                StatutPoche.RESERVEE, referenceDemande);
        if (!dejaReservees.isEmpty()) {
            return allocationsExistantes(dejaReservees);
        }

        List<String> groupesDemandes = groupes.stream()
                .map(GroupeSanguin::name)
                .distinct()
                .toList();
        List<PocheSang> candidates = poches.trouverPochesDisponiblesVerrouillees(
                groupesDemandes, quantite);

        List<PocheDisponible> disponibles = candidates.stream()
                .map(poche -> new PocheDisponible(
                        poche.getId(),
                        poche.getBanque().getBanqueId(),
                        poche.getDatePeremption()))
                .toList();

        Repartition repartition;
        try {
            repartition = calculateurRepartition.calculerRepartition(disponibles, quantite);
        } catch (StockInsuffisantException exception) {
            throw exception;
        }

        Map<String, Allocation> allocations = new LinkedHashMap<>();
        for (LigneRepartition ligne : repartition.lignes()) {
            for (String pocheId : ligne.pocheIds()) {
                PocheSang poche = poches.findById(pocheId).orElseThrow();
                if (poche.getStatut() != StatutPoche.DISPONIBLE
                        || !poche.getDatePeremption().isAfter(LocalDate.now())) {
                    throw new StockInsuffisantException(
                            "Stock insuffisant pour " + quantite + " poche(s)");
                }
                GroupeSanguin groupe = poche.getGroupe();
            poche.reserver(referenceDemande);
            poches.save(poche);
            String cle = poche.getBanque().getBanqueId() + ":" + poche.getGroupe();
            allocations.compute(cle, (unused, allocation) ->
                    allocation == null
                            ? new Allocation(poche.getBanque().getBanqueId(), poche.getGroupe(), 1)
                            : allocation.ajouter());
            }
        }
        publierApresCommit();
        return new ArrayList<>(allocations.values());
    }

    private List<Allocation> allocationsExistantes(List<PocheSang> reservees) {
        Map<String, Allocation> allocations = new LinkedHashMap<>();
        for (PocheSang poche : reservees) {
            String cle = poche.getBanque().getBanqueId() + ":" + poche.getGroupe();
            allocations.compute(cle, (unused, allocation) ->
                    allocation == null
                            ? new Allocation(poche.getBanque().getBanqueId(),
                                    poche.getGroupe(), 1)
                            : allocation.ajouter());
        }
        return new ArrayList<>(allocations.values());
    }

    @Transactional
    public void liberer(String referenceDemande) {
        verifierReference(referenceDemande);
        if (!poches.existsByReferenceReservation(referenceDemande)) {
            throw new ReferenceInconnueException(
                    "Référence inconnue: " + referenceDemande);
        }
        boolean modifie = false;
        for (PocheSang poche : poches.findByStatutAndReferenceReservation(
                StatutPoche.RESERVEE, referenceDemande)) {
            poche.liberer();
            poches.save(poche);
            modifie = true;
        }
        if (modifie) {
            publierApresCommit();
        }
    }

    @Transactional
    public void confirmerSortie(String referenceDemande) {
        verifierReference(referenceDemande);
        if (!poches.existsByReferenceReservation(referenceDemande)) {
            throw new ReferenceInconnueException(
                    "Référence inconnue: " + referenceDemande);
        }

        List<PocheSang> reservees = poches.findByStatutAndReferenceReservation(
                StatutPoche.RESERVEE, referenceDemande);
        if (reservees.isEmpty()) {
            throw new AucunePocheReserveeException(
                    "Aucune poche réservée pour la référence: " + referenceDemande);
        }
        for (PocheSang poche : reservees) {
            poche.confirmerSortie();
            poches.save(poche);
        }
        publierApresCommit();
        publierApresCommit();
    }

    @Transactional
    public PocheSang ajouterPoche(String banqueId, GroupeSanguin groupe, int volumeMl,
                              LocalDate dateCollecte, LocalDate datePeremption) {
        return ajouterPoche("POCHE-" + UUID.randomUUID(), banqueId, groupe,
                volumeMl, dateCollecte, datePeremption);
    }

    @Transactional
    public PocheSang ajouterPoche(String pocheId, String banqueId, GroupeSanguin groupe,
                                  int volumeMl, LocalDate dateCollecte,
                                  LocalDate datePeremption) {
        verifierBanque(banqueId);
        if (groupe == null) {
            throw new GroupeInvalideException("Groupe sanguin invalide");
        }
        if (volumeMl <= 0) {
            throw new GroupeInvalideException("volume_ml doit etre strictement positif");
        }
        if (dateCollecte == null || datePeremption == null) {
            throw new GroupeInvalideException(
                    "Les dates de collecte et de peremption sont obligatoires");
        }
        if (dateCollecte.isAfter(LocalDate.now())) {
            throw new GroupeInvalideException(
                    "La date de collecte ne peut pas etre dans le futur");
        }
        if (!datePeremption.isAfter(dateCollecte)) {
            throw new GroupeInvalideException(
                    "La date de peremption doit etre strictement apres la date de collecte");
        }
        if (regleDePeremption.estPerimee(datePeremption)) {
            throw new PochePerimeeException(
                    "La poche serait deja perimee");
        }
        if (poches.existsById(pocheId)) {
            throw new PocheExistanteException(
                    "Identifiant de poche deja existant: " + pocheId);
        }

        PocheSang poche = new PocheSang(
                pocheId,
                banques.findById(banqueId).orElseThrow(),
                groupe,
                dateCollecte,
                datePeremption);
        PocheSang enregistrée = poches.save(poche);
        publierApresCommit();
        return enregistrée;
    }

    @Transactional(readOnly = true)
    public List<PocheSang> snapshotStocks() {
        return poches.findByStatut(StatutPoche.DISPONIBLE);
    }

    private void verifierBanque(String banqueId) {
        Banque banque = banques.findById(banqueId).orElse(null);
        if (banque == null) {
            throw new BanqueInconnueException("Banque inconnue: " + banqueId);
        }
    }

    private void verifierReference(String referenceDemande) {
        if (referenceDemande == null || referenceDemande.isBlank()) {
            throw new IllegalArgumentException("reference_demande est obligatoire");
        }
    }

    private void publierApresCommit() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            eventPublisher.publishEvent(new StockChangeEvent());
                        }
                    });
        } else {
            eventPublisher.publishEvent(new StockChangeEvent());
        }
    }

    public record Allocation(String banqueId, GroupeSanguin groupe, int quantite) {
        Allocation ajouter() {
            return new Allocation(banqueId, groupe, quantite + 1);
        }
    }
}
