package com.spazepay.util;

import com.spazepay.config.CurrencyConfig; // Import CurrencyConfig
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

@Component
public class CurrencyFormatter {

    private static CurrencyConfig currencyConfig;

    @Autowired
    public void setCurrencyConfig(CurrencyConfig currencyConfig) {
        CurrencyFormatter.currencyConfig = currencyConfig;
    }

    public static String formatCurrency(BigDecimal amount) {
        return formatCurrency(amount, currencyConfig.getDefaultCurrencyCode());
    }

    public static String formatCurrency(BigDecimal amount, String currencyCode) {
        try {
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance(getLocaleForCurrency(currencyCode));
            formatter.setCurrency(currency);
            return formatter.format(amount);
        } catch (IllegalArgumentException e) {
            // Handle unsupported currency code
            return amount.toString() + " " + currencyCode;
        }
    }

    public static String formatCurrency(BigDecimal amount, Currency currency) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(getLocaleForCurrency(currency.getCurrencyCode()));
        formatter.setCurrency(currency);
        return formatter.format(amount);
    }

    private static Locale getLocaleForCurrency(String currencyCode) {
        switch (currencyCode) {
            case "NGN":
                return new Locale("en", "NG");
            // TODO: Add more cases for other currencies and locales here
            default:
                return Locale.getDefault();
        }
    }
}