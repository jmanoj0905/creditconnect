package com.creditconnect.controller;

import com.creditconnect.dto.LoanResponse;
import com.creditconnect.service.OfferService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/offers")
public class OfferController {

    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping("/{id}/accept")
    public LoanResponse accept(@PathVariable Long id) {
        return offerService.acceptOffer(id);
    }
}
