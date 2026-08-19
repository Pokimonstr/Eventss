package ru.top.events.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum EventType {
    PARTY("Вечеринка"),
    HIKE("Поход"),
    MOVIE("Кино"),
    BIRTHDAY("День рождения"),
    DINNER("Ужин"),
    GAME("Игра"),
    CONCERT("Концерт"),
    SPORT("Спорт"),
    OTHER("Другое");

    private final String label;
}
