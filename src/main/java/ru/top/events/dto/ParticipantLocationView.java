package ru.top.events.dto;

public record ParticipantLocationView(
        Long userId,
        String userName,
        Double latitude,
        Double longitude,
        String status,
        String statusLabel,
        String statusIcon,
        Double distanceKm,
        Integer etaMinutes,
        String updatedAt
) {
}
