package ru.top.events.dto;

import java.util.List;

/**
 * Группа фотографий одного дня — результат автогруппировки альбома.
 */
public record PhotoGroupView(
        String label,
        String date,
        List<PhotoView> photos
) {
}
