package com.donsang.inventaire.service;

import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.domain.StatutPoche;
import com.donsang.inventaire.repository.PocheSangRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.LocalDate;

/**
 * T41 / T42 — Règle de péremption des poches.
 *
 * Règle : une poche est périmée DÈS sa date de péremption (le jour même compris).
 * Une poche réservée le jour J doit encore être livrée puis transfusée ; la retirer
 * du stock dès le début du jour J garantit qu'on ne livre jamais une poche invalide (US10).
 *
 * La date du jour vient d'une {@link Clock} injectée (horloge système par défaut),
 * pour que les tests ne dépendent pas de la date réelle.
 */
@Component
public class RegleDePeremption {
    private final PocheSangRepository poches;
    private final ApplicationEventPublisher eventPublisher;
    private final Clock clock;

    /** Utilise le bean {@link Clock} s'il en existe un, sinon l'horloge système. */
    @Autowired
    public RegleDePeremption(PocheSangRepository poches,
                             ApplicationEventPublisher eventPublisher,
                             ObjectProvider<Clock> clock) {
        this(poches, eventPublisher, clock.getIfAvailable(Clock::systemDefaultZone));
    }

    RegleDePeremption(PocheSangRepository poches,
                      ApplicationEventPublisher eventPublisher,
                      Clock clock) {
        this.poches = poches;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    /**
     * T42 — Périmée si la date de péremption est aujourd'hui ou avant.
     * La veille : non périmée ; le jour même et le lendemain : périmée ; date absente : non périmée.
     */
    public boolean estPerimee(LocalDate datePeremption, LocalDate aujourdhui) {
        return datePeremption != null && !datePeremption.isAfter(aujourdhui);
    }

    /** Même règle, avec la date du jour donnée par l'horloge injectée. */
    public boolean estPerimee(LocalDate datePeremption) {
        return estPerimee(datePeremption, LocalDate.now(clock));
    }

    /**
     * T41 — Chaque nuit à minuit (et au démarrage) : passe en PERIMEE les poches DISPONIBLE
     * dont la date est atteinte.
     * Une poche RESERVEE n'est pas touchée : elle appartient à une demande en cours, que
     * Dons confirmera (SORTIE) ou libérera ; libérée, elle redevient DISPONIBLE et sera
     * alors périmée au passage suivant (la réservation refuse déjà les poches périmées).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void appliquerPeremption() {
        LocalDate aujourdhui = LocalDate.now(clock);
        boolean modifie = false;
        for (PocheSang poche : poches.findByStatut(StatutPoche.DISPONIBLE)) {
            if (estPerimee(poche.getDatePeremption(), aujourdhui)) {
                poche.setStatut(StatutPoche.PERIMEE);
                poches.save(poche);
                modifie = true;
            }
        }
        if (modifie) {
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
    }
}
