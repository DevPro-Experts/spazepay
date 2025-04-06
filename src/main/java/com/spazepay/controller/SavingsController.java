package com.spazepay.controller;

import com.spazepay.dto.*;
import com.spazepay.model.SavingsPlan;
import com.spazepay.model.SavingsTransaction;
import com.spazepay.model.User;
import com.spazepay.service.SavingsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/savings/flexible")
public class SavingsController {

    private static final Logger logger = LoggerFactory.getLogger(SavingsController.class);

    @Autowired
    private SavingsService savingsService;

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

    @GetMapping("/active")
    public ResponseEntity<List<SavingsPlan>> getActivePlans(@AuthenticationPrincipal User user) {
        List<SavingsPlan> plans = savingsService.getAllActivePlans(user);
        return ResponseEntity.ok(plans);
    }

    @GetMapping("/{id}")
    public ResponseEntity<SavingsPlan> getPlan(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ResponseEntity.ok(savingsService.getPlanById(user, id));
    }

    @GetMapping("/{planId}/transactions")
    public ResponseEntity<List<SavingsTransaction>> getTransactions(@AuthenticationPrincipal User user,
                                                                    @PathVariable Long planId) {
        return ResponseEntity.ok(savingsService.getTransactionsForPlan(user, planId));
    }
}