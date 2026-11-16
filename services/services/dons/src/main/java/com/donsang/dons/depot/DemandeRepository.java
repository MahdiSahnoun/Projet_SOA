package com.donsang.dons.depot;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.donsang.dons.domaine.Demande;

public interface DemandeRepository extends JpaRepository<Demande, Long> {

    Page<Demande> findByHopitalId(Long hopitalId, Pageable pageable);

    /** Un hôpital ne voit que ses propres demandes. */
    Optional<Demande> findByIdAndHopitalId(Long id, Long hopitalId);
}
