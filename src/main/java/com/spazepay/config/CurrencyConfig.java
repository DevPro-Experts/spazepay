package com.spazepay.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class CurrencyConfig {

    @Value("${app.default.currency}")
    private String defaultCurrencyCode; // e.g., "NGN"

}