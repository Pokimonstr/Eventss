package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.Expense;

import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByEventIdOrderByCreatedAtDesc(Long eventId);
}
