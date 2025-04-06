package com.spazepay.dto.account;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class TopUpAccountRequest {
    @Min(value = 1, message = "Amount must be greater than or equal to 1")
    private int amount;
}
