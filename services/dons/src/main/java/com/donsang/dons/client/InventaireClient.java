package com.donsang.dons.client;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.erreur.InventaireIndisponibleException;
import com.donsang.dons.erreur.RequeteInvalideException;
import com.donsang.dons.erreur.RessourceIntrouvableException;
import com.donsang.dons.erreur.StockInsuffisantException;
import com.donsang.inventaire.v1.AjoutPocheRequest;
import com.donsang.inventaire.v1.InventaireServiceGrpc;
import com.donsang.inventaire.v1.ReferenceRequest;
import com.donsang.inventaire.v1.ReservationReponse;
import com.donsang.inventaire.v1.ReservationRequest;
import com.donsang.inventaire.v1.StockRequest;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import net.devh.boot.grpc.client.inject.GrpcClient;

/**
 * Client gRPC du service Inventaire (appel REST -> gRPC).
 * Chaque appel porte une deadline ; un circuit breaker protège Dons quand Inventaire tombe.
 */
@Service
public class InventaireClient {

    static final String INVENTAIRE = "inventaire";

    @GrpcClient(INVENTAIRE)
    private InventaireServiceGrpc.InventaireServiceBlockingStub stub;

    private final long deadlineMs;

    public InventaireClient(@Value("${inventaire.deadline-ms:2000}") long deadlineMs) {
        this.deadlineMs = deadlineMs;
    }

    /** Deadline côté client : au-delà, l'appel échoue en DEADLINE_EXCEEDED. */
    private InventaireServiceGrpc.InventaireServiceBlockingStub appel() {
        return stub.withDeadlineAfter(deadlineMs, TimeUnit.MILLISECONDS);
    }

    @CircuitBreaker(name = INVENTAIRE, fallbackMethod = "repliReservation")
    public List<AllocationObtenue> reserver(List<GroupeSanguin> groupes, int quantite, String reference) {
        ReservationReponse reponse = appel().reserverPoches(ReservationRequest.newBuilder()
                .addAllGroupes(groupes.stream().map(GroupeMapper::versProto).toList())
                .setQuantite(quantite)
                .setReferenceDemande(reference)
                .build());
        return reponse.getAllocationsList().stream()
                .map(a -> new AllocationObtenue(a.getBanqueId(), GroupeMapper.versDomaine(a.getGroupe()), a.getQuantite()))
                .toList();
    }

    @CircuitBreaker(name = INVENTAIRE, fallbackMethod = "repliLiberation")
    public void liberer(String reference) {
        appel().libererReservation(ReferenceRequest.newBuilder().setReferenceDemande(reference).build());
    }

    @CircuitBreaker(name = INVENTAIRE, fallbackMethod = "repliStock")
    public int stock(GroupeSanguin groupe, String codeBanque) {
        return appel().getStock(StockRequest.newBuilder()
                .setGroupe(GroupeMapper.versProto(groupe))
                .setBanqueId(codeBanque == null ? "" : codeBanque)   // vide = toutes les banques
                .build())
                .getDisponibles();
    }

    @CircuitBreaker(name = INVENTAIRE, fallbackMethod = "repliAjout")
    public String ajouterPoche(String codeBanque, GroupeSanguin groupe, int volumeMl,
                               LocalDate dateCollecte, LocalDate datePeremption) {
        return appel().ajouterPoche(AjoutPocheRequest.newBuilder()
                .setBanqueId(codeBanque)
                .setGroupe(GroupeMapper.versProto(groupe))
                .setVolumeMl(volumeMl)
                .setDateCollecte(dateCollecte.toString())
                .setDatePeremption(datePeremption.toString())
                .build())
                .getPocheId();
    }

    // --- Méthodes de repli : même signature + Throwable --------------------------------

    private List<AllocationObtenue> repliReservation(List<GroupeSanguin> groupes, int quantite, String reference,
                                                     Throwable erreur) {
        throw traduire(erreur);
    }

    private void repliLiberation(String reference, Throwable erreur) {
        throw traduire(erreur);
    }

    private int repliStock(GroupeSanguin groupe, String codeBanque, Throwable erreur) {
        throw traduire(erreur);
    }

    private String repliAjout(String codeBanque, GroupeSanguin groupe, int volumeMl,
                              LocalDate dateCollecte, LocalDate datePeremption, Throwable erreur) {
        throw traduire(erreur);
    }

    /**
     * Codes gRPC métier -> exceptions du domaine ; tout le reste (panne, deadline,
     * circuit ouvert) -> InventaireIndisponibleException (503).
     */
    static RuntimeException traduire(Throwable erreur) {
        if (erreur instanceof StatusRuntimeException s) {
            Status.Code code = s.getStatus().getCode();
            String detail = s.getStatus().getDescription();
            if (code == Status.Code.RESOURCE_EXHAUSTED) {
                return new StockInsuffisantException(detail != null ? detail : "Stock insuffisant");
            }
            if (code == Status.Code.NOT_FOUND) {
                return new RessourceIntrouvableException(detail != null ? detail : "Ressource introuvable dans l'inventaire");
            }
            if (code == Status.Code.INVALID_ARGUMENT) {
                return new RequeteInvalideException(detail != null ? detail : "Requête refusée par l'inventaire");
            }
        }
        return new InventaireIndisponibleException(erreur);
    }
}
