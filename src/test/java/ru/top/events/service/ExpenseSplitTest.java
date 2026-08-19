package ru.top.events.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.top.events.dto.BalanceView;
import ru.top.events.dto.SettlementView;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Разделение счетов: деление суммы и минимизация переводов")
class ExpenseSplitTest {

    @Test
    @DisplayName("Сумма делится поровну, лишние копейки уходят первым участникам")
    void splitsEvenlyWithRemainder() {
        List<BigDecimal> parts = ExpenseService.splitEvenly(new BigDecimal("100.00"), 3);

        assertThat(parts).containsExactly(
                new BigDecimal("33.34"), new BigDecimal("33.33"), new BigDecimal("33.33"));
        assertThat(parts.stream().reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Долги закрываются минимальным числом переводов")
    void minimizesTransfers() {
        List<BalanceView> balances = List.of(
                balance(1L, "Аня", "150.00"),
                balance(2L, "Боря", "-100.00"),
                balance(3L, "Вика", "-50.00"));

        List<SettlementView> settlements = ExpenseService.minimizeTransfers(balances);

        assertThat(settlements).hasSize(2);
        assertThat(settlements).allSatisfy(s -> assertThat(s.toUserId()).isEqualTo(1L));
        assertThat(settlements.stream().map(SettlementView::amount).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("Если все в расчёте — переводов нет")
    void noTransfersWhenSettled() {
        List<BalanceView> balances = List.of(
                balance(1L, "Аня", "0.00"),
                balance(2L, "Боря", "0.00"));

        assertThat(ExpenseService.minimizeTransfers(balances)).isEmpty();
    }

    private BalanceView balance(Long id, String name, String value) {
        BigDecimal amount = new BigDecimal(value);
        return new BalanceView(id, name, amount.max(BigDecimal.ZERO), BigDecimal.ZERO, amount);
    }
}
