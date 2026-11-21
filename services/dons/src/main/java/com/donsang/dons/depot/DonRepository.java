package com.donsang.dons.depot;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.Don;

public interface DonRepository extends JpaRepository<Don, Long> {

    List<Don> findByDonneurIdOrderByDateServiceDesc(Long donneurId);

    /** Un donneur ne voit que ses propres dons. */
    Optional<Don> findByIdAndDonneurId(Long id, Long donneurId);
}
