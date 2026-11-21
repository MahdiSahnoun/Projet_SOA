package com.donsang.dons.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.Donneur;

public interface DonneurRepository extends JpaRepository<Donneur, Long> {

    boolean existsByIdentifiantNational(String identifiantNational);
}
