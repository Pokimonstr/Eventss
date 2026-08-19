package ru.top.events.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum VoteChoice {
    YES("Могу", "👍", 2),
    MAYBE("Возможно", "🤔", 1),
    NO("Не могу", "👎", 0);

    private final String label;
    private final String icon;
    private final int weight;
}
