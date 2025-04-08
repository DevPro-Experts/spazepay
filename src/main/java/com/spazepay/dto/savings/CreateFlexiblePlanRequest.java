package com.spazepay.dto.savings;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;


@Data
@Getter
@Setter
public class CreateFlexiblePlanRequest {
    // Getters and Setters
    @NotBlank(message = "Name is required")
    private String name;

    @Min(value = 1, message = "Initial deposit must be greater than or equal to 1")
    private int initialDeposit;

    @NotBlank(message = "Source is required")
    private String source;

    @NotBlank(message = "Interest handling is required")
    private String interestHandling;

    @NotBlank(message = "Pin is required")
    @Size(min = 4, max = 4, message = "Pin must be 4 digits")
    private String pin;

    @NotNull(message = "maturedAt is required")
    private LocalDateTime maturedAt;

    @Override
    public String toString() {
        return "CreateFlexiblePlanRequest{name='" + name + "', initialDeposit=" + initialDeposit + ", interestHandling='" + interestHandling + "'}";
    }

}
