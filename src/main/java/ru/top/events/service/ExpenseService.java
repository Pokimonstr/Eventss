package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.BalanceView;
import ru.top.events.dto.ExpenseView;
import ru.top.events.dto.SettlementView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.Event;
import ru.top.events.model.Expense;
import ru.top.events.model.ExpenseShare;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.ExpenseRepository;
import ru.top.events.repository.UserRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Разделение счетов: расходы делятся поровну между выбранными участниками,
 * затем считаются балансы и минимальный набор переводов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpenseService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final ExpenseRepository expenseRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipantService participantService;

    @Transactional
    public ExpenseView add(Long eventId, Long payerId, String title, BigDecimal amount, List<Long> sharedWithIds) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Укажи название расхода");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля");
        }

        List<Long> participantIds = (sharedWithIds == null || sharedWithIds.isEmpty())
                ? participantService.participantUsers(eventId).stream().map(User::getId).toList()
                : sharedWithIds;
        if (participantIds.isEmpty()) {
            throw new IllegalArgumentException("Сначала добавь участников мероприятия");
        }

        User payer = userRepository.findById(payerId)
                .orElseThrow(() -> new IllegalArgumentException("Плательщик не найден"));

        Expense expense = Expense.builder()
                .event(event)
                .payer(payer)
                .title(title.trim())
                .amount(amount.setScale(2, RoundingMode.HALF_UP))
                .build();

        List<BigDecimal> parts = splitEvenly(expense.getAmount(), participantIds.size());
        for (int i = 0; i < participantIds.size(); i++) {
            User user = userRepository.findById(participantIds.get(i))
                    .orElseThrow(() -> new IllegalArgumentException("Участник не найден"));
            expense.getShares().add(ExpenseShare.builder()
                    .expense(expense)
                    .user(user)
                    .amount(parts.get(i))
                    .build());
        }

        Expense saved = expenseRepository.save(expense);
        log.info("Добавлен расход id={} на {} ₽ в мероприятии id={}", saved.getId(), saved.getAmount(), eventId);
        return toView(saved);
    }

    @Transactional
    public void delete(Long eventId, Long expenseId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new IllegalArgumentException("Расход не найден"));
        if (!expense.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Расход относится к другому мероприятию");
        }
        expenseRepository.delete(expense);
    }

    @Transactional(readOnly = true)
    public List<ExpenseView> list(Long eventId) {
        return expenseRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(this::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal total(Long eventId) {
        return expenseRepository.findByEventIdOrderByCreatedAtDesc(eventId).stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    @Transactional(readOnly = true)
    public List<BalanceView> balances(Long eventId) {
        Map<Long, String> names = new LinkedHashMap<>();
        Map<Long, BigDecimal> paid = new LinkedHashMap<>();
        Map<Long, BigDecimal> owed = new LinkedHashMap<>();

        for (User user : participantService.participantUsers(eventId)) {
            names.put(user.getId(), user.getDisplayName());
            paid.put(user.getId(), BigDecimal.ZERO);
            owed.put(user.getId(), BigDecimal.ZERO);
        }

        for (Expense expense : expenseRepository.findByEventIdOrderByCreatedAtDesc(eventId)) {
            User payer = expense.getPayer();
            names.putIfAbsent(payer.getId(), payer.getDisplayName());
            paid.merge(payer.getId(), expense.getAmount(), BigDecimal::add);
            owed.putIfAbsent(payer.getId(), BigDecimal.ZERO);

            for (ExpenseShare share : expense.getShares()) {
                User user = share.getUser();
                names.putIfAbsent(user.getId(), user.getDisplayName());
                owed.merge(user.getId(), share.getAmount(), BigDecimal::add);
                paid.putIfAbsent(user.getId(), BigDecimal.ZERO);
            }
        }

        return names.entrySet().stream()
                .map(e -> {
                    BigDecimal p = paid.getOrDefault(e.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                    BigDecimal o = owed.getOrDefault(e.getKey(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
                    return new BalanceView(e.getKey(), e.getValue(), p, o, p.subtract(o));
                })
                .sorted(Comparator.comparing(BalanceView::balance).reversed())
                .toList();
    }

    /**
     * Жадный алгоритм: самый крупный должник платит самому крупному кредитору.
     * Это даёт не больше (N-1) переводов вместо N*(N-1)/2.
     */
    @Transactional(readOnly = true)
    public List<SettlementView> settlements(Long eventId) {
        return minimizeTransfers(balances(eventId));
    }

    static List<SettlementView> minimizeTransfers(List<BalanceView> balances) {
        List<BalanceView> creditors = balances.stream()
                .filter(BalanceView::isCreditor)
                .sorted(Comparator.comparing(BalanceView::balance).reversed())
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        List<BalanceView> debtors = balances.stream()
                .filter(BalanceView::isDebtor)
                .sorted(Comparator.comparing(BalanceView::balance))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        Map<Long, BigDecimal> credit = new LinkedHashMap<>();
        creditors.forEach(c -> credit.put(c.userId(), c.balance()));
        Map<Long, BigDecimal> debt = new LinkedHashMap<>();
        debtors.forEach(d -> debt.put(d.userId(), d.balance().negate()));

        List<SettlementView> transfers = new ArrayList<>();
        int ci = 0;
        int di = 0;
        while (ci < creditors.size() && di < debtors.size()) {
            BalanceView creditor = creditors.get(ci);
            BalanceView debtor = debtors.get(di);
            BigDecimal give = credit.get(creditor.userId()).min(debt.get(debtor.userId()));

            if (give.signum() > 0) {
                transfers.add(new SettlementView(
                        debtor.userId(), debtor.userName(),
                        creditor.userId(), creditor.userName(),
                        give.setScale(2, RoundingMode.HALF_UP)));
                credit.put(creditor.userId(), credit.get(creditor.userId()).subtract(give));
                debt.put(debtor.userId(), debt.get(debtor.userId()).subtract(give));
            }

            if (credit.get(creditor.userId()).compareTo(new BigDecimal("0.01")) < 0) ci++;
            if (debt.get(debtor.userId()).compareTo(new BigDecimal("0.01")) < 0) di++;
        }
        return transfers;
    }

    /**
     * Делит сумму поровну, копейки остатка отдаёт первым участникам.
     */
    static List<BigDecimal> splitEvenly(BigDecimal amount, int parts) {
        BigDecimal base = amount.divide(BigDecimal.valueOf(parts), 2, RoundingMode.DOWN);
        BigDecimal distributed = base.multiply(BigDecimal.valueOf(parts));
        long remainderCents = amount.subtract(distributed).movePointRight(2).longValueExact();

        List<BigDecimal> result = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            BigDecimal share = base;
            if (i < remainderCents) {
                share = share.add(new BigDecimal("0.01"));
            }
            result.add(share);
        }
        return result;
    }

    private ExpenseView toView(Expense expense) {
        List<String> sharedWith = expense.getShares().stream()
                .map(s -> s.getUser().getDisplayName())
                .toList();
        BigDecimal perPerson = sharedWith.isEmpty()
                ? BigDecimal.ZERO
                : expense.getAmount().divide(BigDecimal.valueOf(sharedWith.size()), 2, RoundingMode.HALF_UP);
        return new ExpenseView(
                expense.getId(),
                expense.getTitle(),
                expense.getAmount(),
                expense.getPayer().getDisplayName(),
                expense.getCreatedAt() == null ? null : expense.getCreatedAt().format(FMT),
                sharedWith,
                perPerson);
    }
}
