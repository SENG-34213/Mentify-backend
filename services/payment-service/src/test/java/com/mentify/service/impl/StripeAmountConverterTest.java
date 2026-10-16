package com.mentify.service.impl;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class StripeAmountConverterTest {

    @Test
    void toMinorUnits_forTwoDecimalCurrency_multipliesByOneHundred() {
        assertThat(StripeAmountConverter.toMinorUnits(new BigDecimal("1499.99"), "LKR"))
                .isEqualTo(149999L);
    }

    @Test
    void toMinorUnits_forZeroDecimalCurrency_keepsWholeAmount() {
        assertThat(StripeAmountConverter.toMinorUnits(new BigDecimal("1500"), "JPY"))
                .isEqualTo(1500L);
    }
}
