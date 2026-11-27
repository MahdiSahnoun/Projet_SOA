package com.donsang.inventaire.grpc;

import com.donsang.inventaire.domain.Banque;
import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.notification.EmailNotificationEvent;
import com.donsang.inventaire.repository.BanqueRepository;
import com.donsang.inventaire.repository.PocheSangRepository;
import com.donsang.inventaire.notification.EmailNotificationPublisher;
import com.donsang.inventaire.service.StockChangeEvent;
import com.donsang.inventaire.v1.EtatStock;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class StockStreamBroadcaster {
    private final PocheSangRepository poches;
    private final BanqueRepository banques;
    private final int seuilCritique;
    private final String stockAlertRecipient;
    private final EmailNotificationPublisher notifications;
    private final CopyOnWriteArrayList<Abonnement> abonnements = new CopyOnWriteArrayList<>();
    private final Map<String, Boolean> etatsCritiques = new ConcurrentHashMap<>();

    public StockStreamBroadcaster(PocheSangRepository poches, BanqueRepository banques,
                                  @Value("${inventaire.stock-critique-seuil:3}") int seuilCritique,
                                  @Value("${inventaire.stock-alert-recipient}") String stockAlertRecipient,
                                  EmailNotificationPublisher notifications) {
        this.poches = poches;
        this.banques = banques;
        this.seuilCritique = seuilCritique;
        this.stockAlertRecipient = stockAlertRecipient;
        this.notifications = notifications;
    }

    public void envoyerEtatInitial(String banqueId, GroupeSanguin groupe,
                                   StreamObserver<EtatStock> observer) {
        List<EtatStock> snapshot = snapshot(banqueId, groupe);
        snapshot.forEach(observer::onNext);
    }

    public void ajouterAbonnement(String banqueId, GroupeSanguin groupe,
                                  StreamObserver<EtatStock> observer) {
        Abonnement abonnement = new Abonnement(banqueId, groupe, observer);
        abonnements.add(abonnement);

        if (observer instanceof ServerCallStreamObserver<EtatStock> serveur) {
            serveur.setOnCancelHandler(() -> abonnements.remove(abonnement));
            serveur.setOnCloseHandler(() -> abonnements.remove(abonnement));
        }
    }

    public void retirerAbonnement(StreamObserver<EtatStock> observer) {
        abonnements.removeIf(abonnement -> abonnement.observer() == observer);
    }

    @EventListener
    public void stockAChange(StockChangeEvent event) {
        notifierFranchissementSeuil();
        for (Abonnement abonnement : abonnements) {
            if (abonnement.observer() instanceof ServerCallStreamObserver<EtatStock> serveur
                    && serveur.isCancelled()) {
                abonnements.remove(abonnement);
                continue;
            }
            snapshot(abonnement.banqueId(), abonnement.groupe())
                    .forEach(abonnement.observer()::onNext);
        }
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialiserEtatsCritiques() {
        snapshot(null, null).forEach(etat ->
                etatsCritiques.put(cle(etat), etat.getCritique()));
    }

    private void notifierFranchissementSeuil() {
        snapshot(null, null).forEach(etat -> {
            Boolean etaitCritique = etatsCritiques.put(cle(etat), etat.getCritique());
            if (Boolean.FALSE.equals(etaitCritique) && etat.getCritique()) {
                notifications.publierApresCommit(new EmailNotificationEvent(
                        null,
                        EmailNotificationEvent.Type.STOCK_CRITIQUE,
                        stockAlertRecipient,
                        "Équipe de gestion des stocks",
                        etat.getBanqueId() + ":" + etat.getGroupe().name(),
                        Map.of(
                                "codeBanque", etat.getBanqueId(),
                                "groupeSanguin", etat.getGroupe().name(),
                                "disponibles", Integer.toString(etat.getDisponibles()),
                                "seuil", Integer.toString(seuilCritique))));
            }
        });
    }

    private static String cle(EtatStock etat) {
        return etat.getBanqueId() + ":" + etat.getGroupe().name();
    }

    private List<EtatStock> snapshot(String banqueId, GroupeSanguin groupe) {
        String banqueFiltre = banqueId == null || banqueId.isBlank() ? null : banqueId;
        String groupeFiltre = groupe == null ? null : groupe.name();

        List<PocheSangRepository.StockAggregation> agregats =
                poches.agregerStockDisponible(banqueFiltre, groupeFiltre);
        java.util.Map<String, Integer> disponibles = new java.util.HashMap<>();
        for (PocheSangRepository.StockAggregation agregat : agregats) {
            disponibles.put(agregat.getBanqueId() + ":" + agregat.getGroupe(),
                    (int) agregat.getDisponibles());
        }

        List<String> banquesVisibles = banqueFiltre == null
                ? banques.findAll().stream().map(Banque::getBanqueId).sorted().toList()
                : List.of(banqueFiltre);
        List<GroupeSanguin> groupesVisibles = groupe == null
                ? EnumSet.allOf(GroupeSanguin.class).stream().toList()
                : List.of(groupe);

        List<EtatStock> resultat = new ArrayList<>();
        for (String banque : banquesVisibles) {
            for (GroupeSanguin groupeVisible : groupesVisibles) {
                int nombre = disponibles.getOrDefault(
                        banque + ":" + groupeVisible.name(), 0);
                resultat.add(EtatStock.newBuilder()
                        .setBanqueId(banque)
                        .setGroupe(InventaireGrpcService.versProto(groupeVisible))
                        .setDisponibles(nombre)
                        .setCritique(nombre < seuilCritique)
                        .build());
            }
        }
        return resultat;
    }

    private record Abonnement(String banqueId, GroupeSanguin groupe,
                              StreamObserver<EtatStock> observer) {
    }
}
