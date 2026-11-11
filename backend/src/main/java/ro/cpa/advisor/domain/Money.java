package ro.cpa.advisor.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/** Rotunjirea banilor, intr-un singur loc: toate sumele in euro se rotunjesc la 2 zecimale, in sus de la jumatate. */
final class Money {

    static final MathContext PRECISION = MathContext.DECIMAL128;

    private Money() {}

    static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    static BigDecimal of(double value) {
        return round(BigDecimal.valueOf(value));
    }
}
