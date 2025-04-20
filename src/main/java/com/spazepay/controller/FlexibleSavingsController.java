package com.spazepay.controller;

import com.spazepay.dto.savings.*;
import com.spazepay.dto.transaction.SavingsTransactionResponse;
import com.spazepay.model.User;
import com.spazepay.service.FlexibleSavingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/savings/flexible")
public class FlexibleSavingsController {

    private static final Logger logger = LoggerFactory.getLogger(FlexibleSavingsController.class);

    @Autowired
    private FlexibleSavingsService savingsService;

    @PostMapping("/create")
    public ResponseEntity<SavingsPlanResponse> createFlexiblePlan(@AuthenticationPrincipal User user,
                                                                  @Valid @RequestBody CreateFlexiblePlanRequest request, HttpServletRequest httpRequest) {
        SavingsPlanResponse response = savingsService.createFlexiblePlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/topup")
    public ResponseEntity<TopUpResponse> topUpFlexiblePlan(@AuthenticationPrincipal User user,
                                                           @RequestBody TopUpRequest request) {
        logger.info("Top-up request for plan: {}", request.getPlanId());
        TopUpResponse response = savingsService.topUpFlexiblePlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/withdraw")
    public ResponseEntity<WithdrawResponse> withdrawFlexiblePlan(@AuthenticationPrincipal User user,
                                                                 @Valid @RequestBody WithdrawRequest request) {
        logger.info("Withdraw request for plan: {}", request.getPlanId());
        WithdrawResponse response = savingsService.withdrawFlexiblePlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/liquidate")
    public ResponseEntity<LiquidateResponse> liquidateFlexiblePlan(@AuthenticationPrincipal User user,
                                                                   @Valid @RequestBody LiquidateRequest request) {
        logger.info("Liquidate request for plan: {}", request.getPlanId());
        LiquidateResponse response = savingsService.liquidateFlexiblePlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/rollover/{planId}")
    public ResponseEntity<SavingsPlanResponse> rolloverFlexiblePlan(@AuthenticationPrincipal User user,
                                                                    @PathVariable Long planId,
                                                                    @RequestBody @Valid RolloverRequest rolloverRequest) {
        logger.info("Rollover request for plan: {}", planId);
        SavingsPlanResponse response = savingsService.rolloverFlexiblePlan(user, planId, rolloverRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SavingsPlanResponseLite>> getActivePlansLite(
            @AuthenticationPrincipal User user) {
        List<SavingsPlanResponseLite> plans = savingsService.getAllActivePlansLite(user);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsPlanResponseLite> getPlanLite(
            @AuthenticationPrincipal User user,
            @PathVariable Long id) {
        return ResponseEntity.ok(savingsService.getPlanByIdLite(user, id));
    }

    @GetMapping("/{planId}/transactions")
    public ResponseEntity<List<SavingsTransactionResponse>> getTransactions(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId) {
        logger.info("Fetching transactions for plan: {}", planId);
        List<SavingsTransactionResponse> transactions = savingsService.getTransactionsForPlan(user, planId);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/{planId}/accrued-interest")
    public ResponseEntity<AccruedInterestResponse> getAccruedInterest(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId,
            @Valid @RequestBody AccruedInterestRequest request) {
        logger.info("Fetching accrued interest for plan: {} from {} to {}", planId, request.getStartDate(), request.getEndDate());
        AccruedInterestResponse response = savingsService.getAccruedInterest(user, planId, request);
        return ResponseEntity.ok(response);
    }
}