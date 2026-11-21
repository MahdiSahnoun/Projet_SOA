package com.donsang.dons;

import java.time.LocalDate;
import java.util.UUID;

import com.donsang.dons.domaine.BanqueDeSang;
import com.donsang.dons.domaine.Donneur;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.domaine.Hopital;

/** Objets de test avec des valeurs uniques (la base H2 est partagée entre les tests). */
public final class Fixtures {

    private Fixtures() {
    }

    private static String unique() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static Hopital hopital() {
        Hopital h = new Hopital();
        h.setNom("Hôpital test");
        h.setEmail("hopital-" + unique() + "@test.tn");
        h.setMotDePasse("non-utilise");
        h.setNomEtablissement("CHU test");
        h.setAdresse("Tunis");
        return h;
    }

    public static Donneur donneur() {
        Donneur d = new Donneur();
        d.setNom("Donneur test");
        d.setEmail("donneur-" + unique() + "@test.tn");
        d.setMotDePasse("non-utilise");
        d.setIdentifiantNational("TN-" + unique());
        d.setDateNaissance(LocalDate.of(1995, 3, 10));
        d.setPoids(72f);
        d.setGroupeSanguin(GroupeSanguin.O_NEGATIF);
        return d;
    }

    public static BanqueDeSang banque() {
        BanqueDeSang b = new BanqueDeSang();
        String code = unique();
        b.setNom("Banque " + code);
        b.setEmail("banque-" + code + "@test.tn");
        b.setMotDePasse("non-utilise");
        b.setNomBanque("Banque " + code);
        b.setCodeBanque("BANQUE-" + code);
        return b;
    }
}
