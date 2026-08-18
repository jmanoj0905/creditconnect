package com.creditconnect.controller;

import com.creditconnect.dto.LoanApplicationRequest;
import com.creditconnect.dto.LoanApplicationResponse;
import com.creditconnect.dto.LoanOfferResponse;
import com.creditconnect.dto.RepaymentResponse;
import com.creditconnect.service.LoanApplicationService;
import com.creditconnect.service.RepaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/loans")
public class LoanController {

    private final LoanApplicationService loanApplicationService;
    private final RepaymentService repaymentService;

    public LoanController(LoanApplicationService loanApplicationService,
                          RepaymentService repaymentService) {
        this.loanApplicationService = loanApplicationService;
        this.repaymentService = repaymentService;
    }

    @PostMapping("/apply")
    public ResponseEntity<LoanApplicationResponse> apply(
            @Valid @RequestBody LoanApplicationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(loanApplicationService.apply(request));
    }

    @GetMapping("/{id}")
    public LoanApplicationResponse getApplication(@PathVariable Long id) {
        return loanApplicationService.getApplication(id);
    }

    @GetMapping("/{id}/offers")
    public List<LoanOfferResponse> getOffers(@PathVariable Long id) {
        return loanApplicationService.getOffers(id);
    }

    @PostMapping("/{id}/repay")
    public RepaymentResponse repay(@PathVariable Long id) {
        return repaymentService.repayNextInstalment(id);
    }
}
