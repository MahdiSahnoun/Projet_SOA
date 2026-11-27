package com.donsang.inventaire.grpc;

import com.donsang.inventaire.domain.GroupeSanguin;
import com.donsang.inventaire.domain.PocheSang;
import com.donsang.inventaire.service.InventaireService;
import com.donsang.inventaire.service.StockInsuffisantException;
import com.donsang.inventaire.service.exceptions.AucunePocheReserveeException;
import com.donsang.inventaire.service.exceptions.BanqueInconnueException;
import com.donsang.inventaire.service.exceptions.GroupeInvalideException;
import com.donsang.inventaire.service.exceptions.PocheExistanteException;
import com.donsang.inventaire.service.exceptions.PochePerimeeException;
import com.donsang.inventaire.service.exceptions.ReferenceInconnueException;
import com.donsang.inventaire.v1.Ack;
import com.donsang.inventaire.v1.AjoutPocheRequest;
import com.donsang.inventaire.v1.AllocationBanque;
import com.donsang.inventaire.v1.EtatStock;
import com.donsang.inventaire.v1.InventaireServiceGrpc;
import com.donsang.inventaire.v1.PocheReponse;
import com.donsang.inventaire.v1.ReferenceRequest;
import com.donsang.inventaire.v1.ReservationReponse;
import com.donsang.inventaire.v1.ReservationRequest;
import com.donsang.inventaire.v1.StockReponse;
import com.donsang.inventaire.v1.StockRequest;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@GrpcService
public class InventaireGrpcService extends InventaireServiceGrpc.InventaireServiceImplBase {
    private final InventaireService inventaire;
    private final StockStreamBroadcaster stockStreamBroadcaster;

    public InventaireGrpcService(InventaireService inventaire,
                                 StockStreamBroadcaster stockStreamBroadcaster) {
        this.inventaire = inventaire;
        this.stockStreamBroadcaster = stockStreamBroadcaster;
    }

    @Override
    public void getStock(StockRequest request, StreamObserver<StockReponse> observer) {
        try {
            GroupeSanguin groupe = request.getGroupe()
                    == com.donsang.inventaire.v1.GroupeSanguin.GROUPE_INCONNU
                    ? null
                    : versDomaine(request.getGroupe());
            observer.onNext(StockReponse.newBuilder()
                    .setDisponibles(inventaire.getStock(groupe, normaliserBanque(request.getBanqueId())))
                    .build());
            observer.onCompleted();
        } catch (BanqueInconnueException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void reserverPoches(ReservationRequest request, StreamObserver<ReservationReponse> observer) {
        try {
            List<GroupeSanguin> groupes = request.getGroupesList().stream()
                    .map(InventaireGrpcService::versDomaine)
                    .toList();
            List<InventaireService.Allocation> allocations =
                    inventaire.reserverPoches(
                            request.getReferenceDemande(),
                            groupes,
                            request.getQuantite());

            ReservationReponse.Builder reponse = ReservationReponse.newBuilder();
            for (InventaireService.Allocation allocation : allocations) {
                reponse.addAllocations(AllocationBanque.newBuilder()
                        .setBanqueId(allocation.banqueId())
                        .setGroupe(versProto(allocation.groupe()))
                        .setQuantite(allocation.quantite())
                        .build());
            }
            observer.onNext(reponse.build());
            observer.onCompleted();
        } catch (StockInsuffisantException e) {
            observer.onError(Status.RESOURCE_EXHAUSTED.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void libererReservation(ReferenceRequest request, StreamObserver<Ack> observer) {
        try {
            inventaire.liberer(request.getReferenceDemande());
            observer.onNext(Ack.newBuilder().setOk(true).build());
            observer.onCompleted();
        } catch (ReferenceInconnueException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void confirmerSortie(ReferenceRequest request, StreamObserver<Ack> observer) {
        try {
            inventaire.confirmerSortie(request.getReferenceDemande());
            observer.onNext(Ack.newBuilder().setOk(true).build());
            observer.onCompleted();
        } catch (ReferenceInconnueException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (AucunePocheReserveeException e) {
            observer.onError(Status.FAILED_PRECONDITION.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void ajouterPoche(AjoutPocheRequest request, StreamObserver<PocheReponse> observer) {
        try {
            GroupeSanguin groupe = versDomaine(request.getGroupe());
            verifierGroupe(groupe);
            PocheSang poche = inventaire.ajouterPoche(
                    request.getBanqueId(),
                    groupe,
                    request.getVolumeMl(),
                    LocalDate.parse(request.getDateCollecte()),
                    LocalDate.parse(request.getDatePeremption()));
            observer.onNext(PocheReponse.newBuilder().setPocheId(poche.getId()).build());
            observer.onCompleted();
        } catch (BanqueInconnueException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (PocheExistanteException e) {
            observer.onError(Status.ALREADY_EXISTS.withDescription(e.getMessage()).asRuntimeException());
        } catch (PochePerimeeException | GroupeInvalideException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        } catch (DateTimeParseException e) {
            observer.onError(Status.INVALID_ARGUMENT
                    .withDescription("Date ISO-8601 invalide").asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void streamStocks(StockRequest request, StreamObserver<EtatStock> observer) {
        try {
            String banqueId = normaliserBanque(request.getBanqueId());
            GroupeSanguin groupeDemande = request.getGroupe()
                    != com.donsang.inventaire.v1.GroupeSanguin.GROUPE_INCONNU
                    ? versDomaine(request.getGroupe())
                    : null;
            stockStreamBroadcaster.envoyerEtatInitial(banqueId, groupeDemande, observer);
            stockStreamBroadcaster.ajouterAbonnement(banqueId, groupeDemande, observer);

            if (observer instanceof io.grpc.stub.ServerCallStreamObserver<EtatStock> serveur) {
                serveur.setOnCancelHandler(() -> stockStreamBroadcaster.retirerAbonnement(observer));
                serveur.setOnCloseHandler(() -> stockStreamBroadcaster.retirerAbonnement(observer));
            }
        } catch (BanqueInconnueException e) {
            observer.onError(Status.NOT_FOUND.withDescription(e.getMessage()).asRuntimeException());
        } catch (IllegalArgumentException e) {
            observer.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    private static String normaliserBanque(String banqueId) {
        return banqueId == null || banqueId.isBlank() ? null : banqueId;
    }

    private static void verifierGroupe(GroupeSanguin groupe) {
        if (groupe == null) {
            throw new IllegalArgumentException("Groupe sanguin invalide");
        }
    }

    static GroupeSanguin versDomaine(com.donsang.inventaire.v1.GroupeSanguin groupe) {
        return GroupeSanguin.valueOf(groupe.name());
    }

    public static com.donsang.inventaire.v1.GroupeSanguin versProto(GroupeSanguin groupe) {
        return com.donsang.inventaire.v1.GroupeSanguin.valueOf(groupe.name());
    }
}
