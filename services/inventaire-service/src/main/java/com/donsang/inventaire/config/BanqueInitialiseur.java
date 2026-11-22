package com.donsang.inventaire.config;

import com.donsang.inventaire.domain.Banque;
import com.donsang.inventaire.repository.BanqueRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BanqueInitialiseur implements ApplicationRunner {
    private final BanqueRepository banques;

    public BanqueInitialiseur(BanqueRepository banques) {
        this.banques = banques;
    }

    @Override
    public void run(ApplicationArguments args) {
        enregistrerSiAbsent("BANQUE-A", "Banque A", "Sfax");
        enregistrerSiAbsent("BANQUE-B", "Banque B", "Tunis");
        enregistrerSiAbsent("BANQUE-C", "Banque C", "Sousse");
    }

    private void enregistrerSiAbsent(String banqueId, String nom, String ville) {
        if (banques.findById(banqueId).isEmpty()) {
            banques.save(new Banque(banqueId, nom, ville));
        }
    }
}
