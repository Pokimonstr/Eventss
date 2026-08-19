package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.ParticipantLocation;

import java.util.List;
import java.util.Optional;

public interface ParticipantLocationRepository extends JpaRepository<ParticipantLocation, Long> {
    List<ParticipantLocation> findByEventId(Long eventId);
    Optional<ParticipantLocation> findByEventIdAndUserId(Long eventId, Long userId);
}
