package ru.top.events.dto;

public record ChatMessageView(
        Long id,
        Long authorId,
        String authorName,
        String text,
        String createdAt,
        boolean mine
) {
}
