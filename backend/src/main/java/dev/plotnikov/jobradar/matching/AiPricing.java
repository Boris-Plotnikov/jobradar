package dev.plotnikov.jobradar.matching;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class AiPricing {
    private AiPricing() {}

    record Price(BigDecimal input, BigDecimal output) {}

    static Price priceOf(String model) {
        if (model.contains("haiku")) {
            return new Price(new BigDecimal("0.10"), new BigDecimal("0.50"));
        }
        if (model.contains("sonnet")) {
            return new Price(new BigDecimal("2.00"), new BigDecimal("10.00"));
        }
        if (model.contains("opus")) {
            return new Price(new BigDecimal("4.00"), new BigDecimal("20.00"));
        }
        return new Price(new BigDecimal("2.00"), new BigDecimal("10.00"));
    }

    public static BigDecimal cost(String model, long input, long output, long cacheRead, long cacheWrite) {
        Price p = priceOf(model);
        BigDecimal million = new BigDecimal("1000000");
        BigDecimal total = p.input().multiply(BigDecimal.valueOf(input))
                .add(p.output().multiply(BigDecimal.valueOf(output)))
                .add(p.input().multiply(new BigDecimal("0.10")).multiply(BigDecimal.valueOf(cacheRead)))
                .add(p.input().multiply(new BigDecimal("1.25")).multiply(BigDecimal.valueOf(cacheWrite)));
        return total.divide(million, 6, RoundingMode.HALF_UP);
    }
}
