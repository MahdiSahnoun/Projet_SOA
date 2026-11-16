package com.donsang.dons.client;

import java.util.EnumSet;
import java.util.Set;
import java.util.function.Predicate;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/**
 * Prédicat Resilience4j (ignore-exception-predicate) : une erreur métier d'Inventaire
 * (stock insuffisant, banque inconnue, argument invalide) ne doit pas ouvrir le circuit.
 * Seules les vraies pannes comptent comme échecs.
 */
public class ErreurMetierGrpc implements Predicate<Throwable> {

    private static final Set<Status.Code> PANNES = EnumSet.of(
            Status.Code.UNAVAILABLE,
            Status.Code.DEADLINE_EXCEEDED,
            Status.Code.INTERNAL,
            Status.Code.UNKNOWN,
            Status.Code.CANCELLED);

    @Override
    public boolean test(Throwable erreur) {
        return erreur instanceof StatusRuntimeException s && !PANNES.contains(s.getStatus().getCode());
    }
}
