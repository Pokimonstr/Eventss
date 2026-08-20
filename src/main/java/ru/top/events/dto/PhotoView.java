package ru.top.events.dto;

public record PhotoView(
        Long id,
        String originalName,
        String caption,
        String uploaderName,
        String takenAt,
        String sizeHuman
) {
}
