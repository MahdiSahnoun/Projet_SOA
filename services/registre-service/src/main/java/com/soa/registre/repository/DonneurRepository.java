package com.soa.registre.repository;

import com.soa.registre.domain.Donneur;
import com.soa.registre.domain.GroupeSanguin;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonneurRepository extends JpaRepository<Donneur, String> {

    /**
     * Récupère tous les donneurs d'un groupe sanguin donné.
     * Utilisé par ListerDonneursEligibles (T15).
     */
    List<Donneur> findByGroupeSanguin(GroupeSanguin groupeSanguin);
}
