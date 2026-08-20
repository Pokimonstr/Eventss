package ru.top.events.dto;

import java.math.BigDecimal;
import java.util.List;

public record ExpenseView(
        Long id,
        String title,
        BigDecimal amount,
        String payerName,
        String createdAt,
        List<String> sharedWith,
        BigDecimal amountPerPerson
) {
}
