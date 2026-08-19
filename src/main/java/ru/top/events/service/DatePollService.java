package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.DateOptionView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.DateOption;
import ru.top.events.model.DateVote;
import ru.top.events.model.Event;
import ru.top.events.model.EventStatus;
import ru.top.events.model.User;
import ru.top.events.model.VoteChoice;
import ru.top.events.repository.DateOptionRepository;
import ru.top.events.repository.DateVoteRepository;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Умный выбор даты: участники голосуют за варианты, алгоритм ранжирует их
 * и подсказывает время, когда могут все.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DatePollService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final DateOptionRepository optionRepository;
    private final DateVoteRepository voteRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipantService participantService;

    @Transactional
    public DateOption addOption(Long eventId, LocalDateTime startAt, LocalDateTime endAt) {
        Event event = getEvent(eventId);
        if (startAt == null) {
            throw new IllegalArgumentException("Укажи дату и время начала");
        }
        if (endAt != null && !endAt.isAfter(startAt)) {
            throw new IllegalArgumentException("Окончание должно быть позже начала");
        }
        DateOption option = optionRepository.save(DateOption.builder()
                .event(event)
                .startAt(startAt)
                .endAt(endAt)
                .build());
        log.info("Добавлен вариант даты id={} для мероприятия id={}", option.getId(), eventId);
        return option;
    }

    @Transactional
    public void removeOption(Long eventId, Long optionId) {
        DateOption option = getOption(eventId, optionId);
        optionRepository.delete(option);
    }

    @Transactional
    public void vote(Long eventId, Long optionId, Long userId, VoteChoice choice) {
        DateOption option = getOption(eventId, optionId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        DateVote vote = voteRepository.findByOptionIdAndUserId(optionId, userId)
                .orElseGet(() -> DateVote.builder().option(option).user(user).build());
        vote.setChoice(choice);
        voteRepository.save(vote);
    }

    /**
     * Результаты голосования. Первым идёт лучший вариант:
     * меньше всего отказов, затем больше всего голосов «могу».
     */
    @Transactional(readOnly = true)
    public List<DateOptionView> results(Long eventId) {
        List<DateOption> options = optionRepository.findByEventIdOrderByStartAtAsc(eventId);
        if (options.isEmpty()) {
            return List.of();
        }
        int participantCount = participantService.participantUsers(eventId).size();

        List<DateOptionView> views = new ArrayList<>();
        for (DateOption option : options) {
            List<String> yes = new ArrayList<>();
            List<String> maybe = new ArrayList<>();
            List<String> no = new ArrayList<>();
            Map<Long, VoteChoice> byUser = new HashMap<>();

            for (DateVote vote : option.getVotes()) {
                String name = vote.getUser().getDisplayName();
                byUser.put(vote.getUser().getId(), vote.getChoice());
                switch (vote.getChoice()) {
                    case YES -> yes.add(name);
                    case MAYBE -> maybe.add(name);
                    case NO -> no.add(name);
                }
            }

            int score = yes.size() * VoteChoice.YES.getWeight() + maybe.size() * VoteChoice.MAYBE.getWeight();
            boolean everyoneCan = participantCount > 0 && yes.size() == participantCount;

            views.add(new DateOptionView(
                    option.getId(),
                    option.getStartAt().format(FMT),
                    option.getEndAt() == null ? null : option.getEndAt().format(FMT),
                    yes.size(), maybe.size(), no.size(),
                    score, everyoneCan, false,
                    yes, maybe, no, byUser));
        }

        views.sort(Comparator
                .comparingInt(DateOptionView::noCount)
                .thenComparing(Comparator.comparingInt(DateOptionView::score).reversed())
                .thenComparing(Comparator.comparingInt(DateOptionView::yesCount).reversed()));

        DateOptionView best = views.get(0);
        boolean hasVotes = views.stream().anyMatch(v -> v.yesCount() + v.maybeCount() + v.noCount() > 0);
        if (hasVotes) {
            views.set(0, new DateOptionView(best.id(), best.startAt(), best.endAt(),
                    best.yesCount(), best.maybeCount(), best.noCount(), best.score(),
                    best.everyoneCan(), true, best.yesNames(), best.maybeNames(), best.noNames(),
                    best.votesByUserId()));
        }
        return views;
    }

    /**
     * Фиксирует выбранный вариант как дату мероприятия.
     */
    @Transactional
    public void confirm(Long eventId, Long optionId) {
        DateOption option = getOption(eventId, optionId);
        Event event = option.getEvent();
        event.setStartAt(option.getStartAt());
        event.setEndAt(option.getEndAt());
        event.setStatus(EventStatus.CONFIRMED);
        eventRepository.save(event);
        log.info("Мероприятие id={} подтверждено на {}", eventId, option.getStartAt());
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private DateOption getOption(Long eventId, Long optionId) {
        DateOption option = optionRepository.findById(optionId)
                .orElseThrow(() -> new IllegalArgumentException("Вариант даты не найден"));
        if (!option.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Вариант относится к другому мероприятию");
        }
        return option;
    }
}
