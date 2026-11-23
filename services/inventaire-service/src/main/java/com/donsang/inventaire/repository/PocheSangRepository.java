package com.donsang.inventaire.repository;

import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.domain.StatutPoche;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface PocheSangRepository extends JpaRepository<PocheSang, String> {
    long countByGroupeAndStatut(GroupeSanguin groupe, StatutPoche statut);
    long countByBanqueBanqueIdAndGroupeAndStatut(
            String banqueId, GroupeSanguin groupe, StatutPoche statut);
    List<PocheSang> findByStatutAndReferenceReservation(
            StatutPoche statut, String referenceReservation);
    List<PocheSang> findByStatutAndBanqueBanqueIdAndGroupe(
            StatutPoche statut, String banqueId, GroupeSanguin groupe);
    List<PocheSang> findByStatutAndGroupe(StatutPoche statut, GroupeSanguin groupe);
    List<PocheSang> findByStatut(StatutPoche statut);
    boolean existsByReferenceReservation(String referenceReservation);
    Optional<PocheSang> findFirstByReferenceReservation(String referenceReservation);

    @Query(value = """
            SELECT p.*
            FROM poches p
            WHERE p.statut = 'DISPONIBLE'
              AND p.date_peremption > CURRENT_DATE
              AND p.groupe IN (:groupes)
            ORDER BY p.date_peremption ASC
            FOR UPDATE OF p SKIP LOCKED
            """, nativeQuery = true)
    List<PocheSang> trouverPochesDisponiblesVerrouillees(
            @Param("groupes") List<String> groupes);

    @Query(value = """
            SELECT p.banque_id AS banqueId,
                   p.groupe AS groupe,
                   COUNT(*) AS disponibles
            FROM poches p
            WHERE p.statut = 'DISPONIBLE'
              AND p.date_peremption > CURRENT_DATE
              AND (:banqueId IS NULL OR p.banque_id = :banqueId)
              AND (:groupe IS NULL OR p.groupe = :groupe)
            GROUP BY p.banque_id, p.groupe
            """, nativeQuery = true)
    List<StockAggregation> agregerStockDisponible(
            @Param("banqueId") String banqueId,
            @Param("groupe") String groupe);

    interface StockAggregation {
        String getBanqueId();
        String getGroupe();
        long getDisponibles();
    }
}
