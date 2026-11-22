package com.donsang.dons.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.donsang.dons.api.dto.StockReponse;
import com.donsang.dons.domaine.GroupeSanguin;
import com.donsang.dons.service.StockService;

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
@Tag(name = "Stock")
public class StockController {

    private final StockService stockService;

    /** ?groupeSanguin=O_NEGATIF[&codeBanque=BANQUE-A] ; sans codeBanque : toutes les banques. */
    @GetMapping
    public StockReponse consulter(@RequestParam GroupeSanguin groupeSanguin,
                                  @RequestParam(required = false) String codeBanque) {
        return stockService.stock(groupeSanguin, codeBanque);
    }
}
