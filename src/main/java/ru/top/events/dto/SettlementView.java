package ru.top.events.dto;

import java.math.BigDecimal;

/**
 * Один перевод: кто, кому и сколько должен отправить.
 */
public record SettlementView(
        Long fromUserId,
        String fromName,
        Long toUserId,
        String toName,
        BigDecimal amount
) {
}
