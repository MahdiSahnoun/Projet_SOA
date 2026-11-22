package com.donsang.dons.depot;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.BanqueDeSang;

public interface BanqueDeSangRepository extends JpaRepository<BanqueDeSang, Long> {

    boolean existsByCodeBanque(String codeBanque);
}
