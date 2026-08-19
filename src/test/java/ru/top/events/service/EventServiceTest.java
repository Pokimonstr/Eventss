package ru.top.events.service;
import org.springframework.data.domain.Pageable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import ru.top.events.dto.EventFormDto;
import ru.top.events.dto.EventView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.*;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Unit Tests")
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private EventService eventService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("demo")
                .email("demo@test.ru")
                .password("password")
                .displayName("Демо Пользователь")
                .build();

        testEvent = Event.builder()
                .id(1L)
                .owner(testUser)
                .title("Тестовое мероприятие")
                .description("Описание")
                .type(EventType.PARTY)
                .status(EventStatus.PLANNING)
                .startAt(LocalDateTime.now().plusDays(7))
                .locationName("Парк Горького")
                .maxParticipants(10)
                .budgetAmount(new BigDecimal("5000"))
                .build();
    }

    @Test
    @DisplayName("findById должен возвращать мероприятие по ID")
    void findById_shouldReturnEvent_whenEventExists() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        EventView result = eventService.findById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getTitle()).isEqualTo("Тестовое мероприятие");
        assertThat(result.getType()).isEqualTo(EventType.PARTY);
        assertThat(result.getOwnerName()).isEqualTo("Демо Пользователь");
        verify(eventRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findById должен бросать EventNotFoundException, если нет такого ID")
    void findById_shouldThrowException_whenEventNotFound() {
        when(eventRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> eventService.findById(999L))
                .isInstanceOf(EventNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("create должен сохранять мероприятие и добавлять организатора как участника")
    void create_shouldSaveEvent_andAddOwnerAsParticipant() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> {
            Event e = invocation.getArgument(0);
            e.setId(1L);
            return e;
        });

        EventFormDto form = new EventFormDto();
        form.setTitle("Новая тусовка");
        form.setType(EventType.PARTY);
        form.setLocationName("Москва");

        EventView result = eventService.create(form);

        assertThat(result.getTitle()).isEqualTo("Новая тусовка");
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("findAll должен возвращать страницу мероприятий")
    void findAll_shouldReturnPageOfEvents() {
        Page<Event> page = new PageImpl<>(List.of(testEvent));

        // Явно указываем, что мокаем метод с Pageable, а не с Sort
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        Page<EventView> result = eventService.findAll(
                null, null, null, PageRequest.of(0, 6));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("Тестовое мероприятие");
    }

    @Test
    @DisplayName("toFormDto должен возвращать все поля включая даты")
    void toFormDto_shouldReturnAllFieldsIncludingDates() {
        LocalDateTime start = LocalDateTime.of(2026, 8, 20, 18, 0);
        LocalDateTime end = LocalDateTime.of(2026, 8, 20, 22, 0);
        testEvent.setStartAt(start);
        testEvent.setEndAt(end);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        EventFormDto form = eventService.toFormDto(1L);

        assertThat(form.getTitle()).isEqualTo("Тестовое мероприятие");
        assertThat(form.getStartAt()).isEqualTo(start);
        assertThat(form.getEndAt()).isEqualTo(end);
    }

    @Test
    @DisplayName("delete должен удалять мероприятие")
    void delete_shouldRemoveEvent() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));

        eventService.delete(1L);

        verify(eventRepository, times(1)).delete(testEvent);
    }

    @Test
    @DisplayName("update должен обновлять поля мероприятия")
    void update_shouldUpdateEventFields() {
        when(eventRepository.findById(1L)).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any(Event.class))).thenReturn(testEvent);

        EventFormDto form = new EventFormDto();
        form.setTitle("Обновлённое название");
        form.setType(EventType.MOVIE);
        form.setLocationName("Кинотеатр");

        EventView result = eventService.update(1L, form);

        assertThat(result.getTitle()).isEqualTo("Обновлённое название");
        verify(eventRepository, times(1)).save(any(Event.class));
    }
}