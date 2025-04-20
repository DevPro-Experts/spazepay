package com.spazepay.controller;

import com.spazepay.dto.savings.*;
import com.spazepay.model.User;
import com.spazepay.service.TargetSavingsService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/savings/target")
public class TargetSavingsController {
    private static final Logger logger = LoggerFactory.getLogger(TargetSavingsController.class);

    @Autowired
    private TargetSavingsService targetSavingsService;

    @PostMapping("/create")
    public ResponseEntity<SavingsPlanResponse> createTargetPlan(@AuthenticationPrincipal User user,
                                                                @Valid @RequestBody CreateTargetPlanRequest request) {
        logger.info("Creating target plan for user: {}", user.getId());
        SavingsPlanResponse response = targetSavingsService.createTargetPlan(user, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{planId}/accrued-interest")
    public ResponseEntity<AccruedInterestResponse> getAccruedInterest(
            @AuthenticationPrincipal User user,
            @PathVariable Long planId,
            @Valid @RequestBody AccruedInterestRequest request) {
        logger.info("Fetching accrued interest for target plan: {}", planId);
        AccruedInterestResponse response = targetSavingsService.getAccruedInterest(user, planId, request);
        return ResponseEntity.ok(response);
    }
}