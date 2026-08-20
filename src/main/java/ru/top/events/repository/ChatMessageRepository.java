package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.ChatMessage;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByEventIdOrderByIdAsc(Long eventId);
    List<ChatMessage> findByEventIdAndIdGreaterThanOrderByIdAsc(Long eventId, Long afterId);
    long countByEventId(Long eventId);
}
