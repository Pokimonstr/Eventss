package ru.top.events.dto;

import ru.top.events.model.EventParticipant;

public record ParticipantView(
        Long participantId,
        Long userId,
        String displayName,
        EventParticipant.ParticipantRole role,
        EventParticipant.RsvpStatus rsvpStatus
) {
}
