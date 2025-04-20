package com.spazepay.controller;

import com.spazepay.dto.savings.*;
import com.spazepay.model.User;
import com.spazepay.service.FixedSavingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/savings/fixed")
public class FixedSavingsController {
    private static final Logger logger = LoggerFactory.getLogger(FixedSavingsController.class);

    @Autowired
    private FixedSavingsService fixedSavingsService;

    @PostMapping("/create")
    public ResponseEntity<SavingsPlanResponse> createFixedPlan(@AuthenticationPrincipal User user,
                                                               @Valid @RequestBody CreateFixedPlanRequest request) {
        logger.info("Creating fixed plan for user: {}", user.getId());
        SavingsPlanResponse response = fixedSavingsService.createFixedPlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{planId}/accrued-interest")
    public ResponseEntity<AccruedInterestResponse> getAccruedInterest(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId,
            @Valid @RequestBody AccruedInterestRequest request) {
        logger.info("Fetching accrued interest for fixed plan: {}", planId);
        AccruedInterestResponse response = fixedSavingsService.getAccruedInterest(user, planId, request);
        return ResponseEntity.ok(response);
    }
}