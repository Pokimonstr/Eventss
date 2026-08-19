package ru.top.events.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventStatus {
    PLANNING("Планирование"),
    CONFIRMED("Подтверждено"),
    ACTIVE("Идёт сейчас"),
    COMPLETED("Завершено"),
    CANCELLED("Отменено");

    private final String label;
}
