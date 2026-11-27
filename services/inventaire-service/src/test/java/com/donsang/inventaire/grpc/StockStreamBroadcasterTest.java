package com.donsang.inventaire.grpc;

import com.donsang.inventaire.domain.Banque;
import com.donsang.inventaire.notification.EmailNotificationEvent;
import com.donsang.inventaire.notification.EmailNotificationPublisher;
import com.donsang.inventaire.repository.BanqueRepository;
import com.donsang.inventaire.repository.PocheSangRepository;
import com.donsang.inventaire.service.StockChangeEvent;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StockStreamBroadcasterTest {

    @Test
    void publieUneAlerteUneSeuleFoisAuFranchissementDuSeuilCritique() {
        PocheSangRepository poches = mock(PocheSangRepository.class);
        BanqueRepository banques = mock(BanqueRepository.class);
        EmailNotificationPublisher notifications = mock(EmailNotificationPublisher.class);
        AtomicLong disponibles = new AtomicLong(4);
        Banque banque = mock(Banque.class);
        when(banque.getBanqueId()).thenReturn("BANQUE-A");
        when(banques.findAll()).thenReturn(List.of(banque));
        PocheSangRepository.StockAggregation stock = mock(PocheSangRepository.StockAggregation.class);
        when(stock.getBanqueId()).thenReturn("BANQUE-A");
        when(stock.getGroupe()).thenReturn("O_NEGATIF");
        when(stock.getDisponibles()).thenAnswer(invocation -> disponibles.get());
        when(poches.agregerStockDisponible(isNull(), isNull())).thenReturn(List.of(stock));

        StockStreamBroadcaster broadcaster =
                new StockStreamBroadcaster(poches, banques, 3, "stock-alerts@example.test", notifications);
        broadcaster.initialiserEtatsCritiques();

        disponibles.set(2);
        broadcaster.stockAChange(new StockChangeEvent());
        broadcaster.stockAChange(new StockChangeEvent());

        verify(notifications, times(1)).publierApresCommit(argThat(event ->
                event.type() == EmailNotificationEvent.Type.STOCK_CRITIQUE
                        && event.recipientEmail().equals("stock-alerts@example.test")
                        && event.data().get("disponibles").equals("2")));
    }

    @Test
    void nePubliePasUneAlerteSiLeStockResteAuDessusDuSeuil() {
        PocheSangRepository poches = mock(PocheSangRepository.class);
        BanqueRepository banques = mock(BanqueRepository.class);
        EmailNotificationPublisher notifications = mock(EmailNotificationPublisher.class);
        Banque banque = mock(Banque.class);
        when(banque.getBanqueId()).thenReturn("BANQUE-A");
        when(banques.findAll()).thenReturn(List.of(banque));
        PocheSangRepository.StockAggregation stock = mock(PocheSangRepository.StockAggregation.class);
        when(stock.getBanqueId()).thenReturn("BANQUE-A");
        when(stock.getGroupe()).thenReturn("O_NEGATIF");
        when(stock.getDisponibles()).thenReturn(4L);
        when(poches.agregerStockDisponible(isNull(), isNull())).thenReturn(List.of(stock));

        StockStreamBroadcaster broadcaster =
                new StockStreamBroadcaster(poches, banques, 3, "stock-alerts@example.test", notifications);
        broadcaster.initialiserEtatsCritiques();
        broadcaster.stockAChange(new StockChangeEvent());

        verify(notifications, never()).publierApresCommit(argThat(event ->
                event.type() == EmailNotificationEvent.Type.STOCK_CRITIQUE));
    }
}
