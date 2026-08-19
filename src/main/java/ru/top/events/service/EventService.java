package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.EventFormDto;
import ru.top.events.dto.EventView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.Event;
import ru.top.events.model.EventParticipant;
import ru.top.events.model.EventStatus;
import ru.top.events.model.EventType;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.EventSpecifications;
import ru.top.events.repository.UserRepository;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final long DEMO_OWNER_ID = 1L;

    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Page<EventView> findAll(String search, EventType type, EventStatus status, Pageable pageable) {
        log.info("Запрос списка мероприятий: search={}, type={}, status={}, page={}",
                search, type, status, pageable.getPageNumber());

        Specification<Event> spec = EventSpecifications.withFilters(search, type, status);

        return eventRepository.findAll(spec, pageable)
                .map(this::toView);
    }

    @Transactional(readOnly = true)
    public EventView findById(Long id) {
        log.info("Запрос мероприятия id={}", id);
        return toView(getEvent(id));
    }

    @Transactional(readOnly = true)
    public EventFormDto toFormDto(Long id) {
        Event event = getEvent(id);
        EventFormDto form = new EventFormDto();
        form.setId(event.getId());
        form.setTitle(event.getTitle());
        form.setDescription(event.getDescription());
        form.setType(event.getType());
        form.setStartAt(event.getStartAt());
        form.setEndAt(event.getEndAt());
        form.setLocationName(event.getLocationName());
        form.setMaxParticipants(event.getMaxParticipants());
        form.setBudgetAmount(event.getBudgetAmount());
        return form;
    }

    @Transactional
    public EventView create(EventFormDto form) {
        User owner = getDemoOwner();

        Event event = Event.builder()
                .owner(owner)
                .title(form.getTitle())
                .description(form.getDescription())
                .type(form.getType())
                .status(EventStatus.PLANNING)
                .startAt(form.getStartAt())
                .endAt(form.getEndAt())
                .locationName(form.getLocationName())
                .maxParticipants(form.getMaxParticipants())
                .budgetAmount(form.getBudgetAmount())
                .build();

        EventParticipant participant = EventParticipant.builder()
                .event(event)
                .user(owner)
                .role(EventParticipant.ParticipantRole.OWNER)
                .rsvpStatus(EventParticipant.RsvpStatus.CONFIRMED)
                .build();
        event.getParticipants().add(participant);

        Event saved = eventRepository.save(event);
        log.info("Создано мероприятие id={} title='{}'", saved.getId(), saved.getTitle());
        return toView(saved);
    }

    @Transactional
    public EventView update(Long id, EventFormDto form) {
        Event event = getEvent(id);
        event.setTitle(form.getTitle());
        event.setDescription(form.getDescription());
        event.setType(form.getType());
        event.setStartAt(form.getStartAt());
        event.setEndAt(form.getEndAt());
        event.setLocationName(form.getLocationName());
        event.setMaxParticipants(form.getMaxParticipants());
        event.setBudgetAmount(form.getBudgetAmount());

        Event saved = eventRepository.save(event);
        log.info("Обновлено мероприятие id={}", id);
        return toView(saved);
    }

    @Transactional
    public void delete(Long id) {
        Event event = getEvent(id);
        eventRepository.delete(event);
        log.info("Удалено мероприятие id={} title='{}'", id, event.getTitle());
    }

    private Event getEvent(Long id) {
        return eventRepository.findById(id)
                .orElseThrow(() -> new EventNotFoundException(id));
    }

    private User getDemoOwner() {
        return userRepository.findById(DEMO_OWNER_ID)
                .orElseThrow(() -> new RuntimeException("Демо-пользователь не найден"));
    }

    private EventView toView(Event e) {
        String start = e.getStartAt() == null ? null : e.getStartAt().format(FMT);
        String end = e.getEndAt() == null ? null : e.getEndAt().format(FMT);
        int count = e.getParticipants() == null ? 0 : e.getParticipants().size();
        return new EventView(
                e.getId(), e.getTitle(), e.getDescription(),
                e.getType(), e.getStatus(), start, end,
                e.getLocationName(), e.getLatitude(), e.getLongitude(),
                e.getMaxParticipants(), e.getBudgetAmount(),
                e.getOwner().getDisplayName(), count
        );
    }
}
