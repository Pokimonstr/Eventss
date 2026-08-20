package ru.top.events.dto;

import java.math.BigDecimal;

/**
 * Итог по участнику: сколько заплатил, сколько должен и что в остатке.
 */
public record BalanceView(
        Long userId,
        String userName,
        BigDecimal paid,
        BigDecimal owed,
        BigDecimal balance
) {
    public boolean isCreditor() {
        return balance.signum() > 0;
    }

    public boolean isDebtor() {
        return balance.signum() < 0;
    }
}
