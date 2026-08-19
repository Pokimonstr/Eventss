package ru.top.events.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TravelStatus {
    NOT_STARTED("Ещё дома", "🏠"),
    ON_THE_WAY("В пути", "🚶"),
    NEARBY("Рядом", "📍"),
    ARRIVED("На месте", "✅"),
    LATE("Опаздывает", "⏰");

    private final String label;
    private final String icon;
}
