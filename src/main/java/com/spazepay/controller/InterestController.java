package com.spazepay.controller;

import com.spazepay.service.InterestEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interest")
public class InterestController {
    @Autowired
    private InterestEngine interestEngine;

    @PostMapping("/calculate-daily")
    public ResponseEntity<String> calculateDailyInterest() {
        interestEngine.applyDailyInterest();
        return ResponseEntity.ok("Daily interest applied");
    }
}