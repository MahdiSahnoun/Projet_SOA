package com.donsang.inventaire.service;

import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.domain.StatutPoche;
import com.donsang.inventaire.repository.PocheSangRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;

@Component
public class RegleDePeremption {
    private final PocheSangRepository poches;
    private final ApplicationEventPublisher eventPublisher;

    public RegleDePeremption(PocheSangRepository poches,
                             ApplicationEventPublisher eventPublisher) {
        this.poches = poches;
        this.eventPublisher = eventPublisher;
    }

    public boolean estPerimee(LocalDate datePeremption) {
        return datePeremption != null && !datePeremption.isAfter(LocalDate.now());
    }

    @EventListener(ApplicationReadyEvent.class)
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void appliquerPeremption() {
        boolean modifie = false;
        for (PocheSang poche : poches.findAll()) {
            if (poche.getStatut() != StatutPoche.PERIMEE
                    && poche.getStatut() != StatutPoche.SORTIE
                    && estPerimee(poche.getDatePeremption())) {
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
