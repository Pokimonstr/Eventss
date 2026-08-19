package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.ParticipantView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.Event;
import ru.top.events.model.EventParticipant;
import ru.top.events.model.User;
import ru.top.events.repository.EventParticipantRepository;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParticipantService {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ParticipantView> list(Long eventId) {
        return participantRepository.findByEventId(eventId).stream()
                .sorted(Comparator.comparing(EventParticipant::getId))
                .map(p -> new ParticipantView(
                        p.getId(),
                        p.getUser().getId(),
                        p.getUser().getDisplayName(),
                        p.getRole(),
                        p.getRsvpStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<User> participantUsers(Long eventId) {
        return participantRepository.findByEventId(eventId).stream()
                .sorted(Comparator.comparing(EventParticipant::getId))
                .map(EventParticipant::getUser)
                .toList();
    }

    /**
     * Добавляет участника по имени. Если такого пользователя ещё нет — создаёт его.
     */
    @Transactional
    public ParticipantView add(Long eventId, String displayName) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EventNotFoundException(eventId));

        String name = displayName == null ? "" : displayName.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Имя участника не может быть пустым");
        }

        User user = userRepository.findByUsername(usernameOf(name))
                .orElseGet(() -> createUser(name));

        participantRepository.findByEventIdAndUserId(eventId, user.getId())
                .ifPresent(p -> {
                    throw new IllegalArgumentException("Участник уже добавлен");
                });

        EventParticipant participant = participantRepository.save(EventParticipant.builder()
                .event(event)
                .user(user)
                .role(EventParticipant.ParticipantRole.PARTICIPANT)
                .rsvpStatus(EventParticipant.RsvpStatus.CONFIRMED)
                .build());

        log.info("В мероприятие id={} добавлен участник '{}'", eventId, name);
        return new ParticipantView(participant.getId(), user.getId(), user.getDisplayName(),
                participant.getRole(), participant.getRsvpStatus());
    }

    @Transactional
    public void remove(Long eventId, Long participantId) {
        EventParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Участник не найден"));
        if (!participant.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Участник относится к другому мероприятию");
        }
        if (participant.getRole() == EventParticipant.ParticipantRole.OWNER) {
            throw new IllegalArgumentException("Организатора нельзя удалить");
        }
        participantRepository.delete(participant);
    }

    @Transactional
    public void setRsvp(Long eventId, Long participantId, EventParticipant.RsvpStatus status) {
        EventParticipant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new IllegalArgumentException("Участник не найден"));
        if (!participant.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Участник относится к другому мероприятию");
        }
        participant.setRsvpStatus(status);
        participantRepository.save(participant);
    }

    private User createUser(String displayName) {
        String base = usernameOf(displayName);
        String username = base;
        int suffix = 2;
        while (userRepository.existsByUsername(username)) {
            username = base + suffix++;
        }
        return userRepository.save(User.builder()
                .username(username)
                .email(username + "@letsgo.local")
                .password("demo")
                .displayName(displayName)
                .build());
    }

    private String usernameOf(String displayName) {
        String slug = displayName.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }
}
