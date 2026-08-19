package ru.top.events.repository;

import ru.top.events.model.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface EventParticipantRepository
        extends JpaRepository<EventParticipant, Long> {

    List<EventParticipant> findByEventId(Long eventId);
    Optional<EventParticipant> findByEventIdAndUserId(Long eventId, Long userId);
    long countByEventIdAndRsvpStatus(
            Long eventId,
            EventParticipant.RsvpStatus status
    );
}