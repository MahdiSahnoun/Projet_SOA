package com.donsang.inventaire.repartition;

import java.time.LocalDate;

public record PocheDisponible(String id, String banqueId, LocalDate datePeremption) {
}
