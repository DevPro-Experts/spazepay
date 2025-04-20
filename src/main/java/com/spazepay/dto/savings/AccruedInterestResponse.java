package com.spazepay.dto.savings;

import lombok.Data;

@Data
public class AccruedInterestResponse {
    private String planId;
    private String totalAccruedInterest;
    private String startDate;
    private String endDate;

    public AccruedInterestResponse(String planId, String totalAccruedInterest, String startDate, String endDate) {
        this.planId = planId;
        this.totalAccruedInterest = totalAccruedInterest;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    // Getters and setters
}