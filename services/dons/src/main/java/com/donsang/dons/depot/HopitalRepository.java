package com.donsang.dons.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.Hopital;

public interface HopitalRepository extends JpaRepository<Hopital, Long> {
}
