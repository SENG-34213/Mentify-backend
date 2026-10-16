package com.mentify.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Set;

final class StripeAmountConverter {

    private static final Set<String> ZERO_DECIMAL_CURRENCIES = Set.of(
            "BIF", "CLP", "DJF", "GNF", "JPY", "KMF", "KRW", "MGA",
            "PYG", "RWF", "UGX", "VND", "VUV", "XAF", "XOF", "XPF"
    );

    private StripeAmountConverter() {
    }

    static long toMinorUnits(BigDecimal amount, String currency) {
        String normalizedCurrency = currency.toUpperCase(Locale.ROOT);
        BigDecimal multiplier = ZERO_DECIMAL_CURRENCIES.contains(normalizedCurrency)
                ? BigDecimal.ONE
                : new BigDecimal("100");

        return amount.multiply(multiplier)
                .setScale(0, RoundingMode.UNNECESSARY)
                .longValueExact();
    }
}
