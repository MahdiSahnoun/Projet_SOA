package com.donsang.dons.depot;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.Utilisateur;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Long> {

    Optional<Utilisateur> findByEmail(String email);

    boolean existsByEmail(String email);
}
