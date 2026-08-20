package ru.top.events.dto;

public record LocationSuggestion(
        String name,
        String description,
        String priceHint,
        int pricePerPerson
) {
}
