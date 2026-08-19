package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.ChatMessageView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.ChatMessage;
import ru.top.events.model.Event;
import ru.top.events.model.User;
import ru.top.events.repository.ChatMessageRepository;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM HH:mm");
    private static final int MAX_LENGTH = 2000;

    private final ChatMessageRepository messageRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChatMessageView post(Long eventId, Long authorId, String text) {
        Event event = eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
        String body = text == null ? "" : text.trim();
        if (body.isEmpty()) {
            throw new IllegalArgumentException("Сообщение не может быть пустым");
        }
        if (body.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Сообщение слишком длинное");
        }
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new IllegalArgumentException("Автор не найден"));

        ChatMessage message = messageRepository.save(ChatMessage.builder()
                .event(event)
                .author(author)
                .text(body)
                .build());
        return toView(message, authorId);
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> history(Long eventId, Long currentUserId) {
        return messageRepository.findByEventIdOrderByIdAsc(eventId).stream()
                .map(m -> toView(m, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ChatMessageView> since(Long eventId, Long afterId, Long currentUserId) {
        if (afterId == null || afterId <= 0) {
            return history(eventId, currentUserId);
        }
        return messageRepository.findByEventIdAndIdGreaterThanOrderByIdAsc(eventId, afterId).stream()
                .map(m -> toView(m, currentUserId))
                .toList();
    }

    @Transactional(readOnly = true)
    public long count(Long eventId) {
        return messageRepository.countByEventId(eventId);
    }

    private ChatMessageView toView(ChatMessage m, Long currentUserId) {
        Long authorId = m.getAuthor().getId();
        return new ChatMessageView(
                m.getId(),
                authorId,
                m.getAuthor().getDisplayName(),
                m.getText(),
                m.getCreatedAt().format(FMT),
                authorId.equals(currentUserId));
    }
}
