package ru.top.events.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import ru.top.events.model.Event;
import ru.top.events.model.EventType;
import ru.top.events.model.EventStatus;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("EventController Integration Tests")
class EventControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User user = userRepository.save(User.builder()
                .username("testuser")
                .email("test@test.ru")
                .password("pass")
                .displayName("Тестовый юзер")
                .build());

        Event event = Event.builder()
                .owner(user)
                .title("Тестовая вечеринка")
                .description("Описание")
                .type(EventType.PARTY)
                .status(EventStatus.CONFIRMED)
                .startAt(LocalDateTime.now().plusDays(3))
                .locationName("Клуб")
                .budgetAmount(new BigDecimal("3000"))
                .build();
        eventRepository.save(event);
    }

    @Test
    @DisplayName("GET /events должен возвращать страницу со статусом 200")
    void list_shouldReturnOk() throws Exception {
        mockMvc.perform(get("/events"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/list"))
                .andExpect(model().attributeExists("events"))
                .andExpect(model().attributeExists("types"))
                .andExpect(model().attributeExists("statuses"));
    }

    @Test
    @DisplayName("GET /events?search=вечеринка должен фильтровать по названию")
    void list_withSearchFilter_shouldReturnFilteredEvents() throws Exception {
        mockMvc.perform(get("/events").param("search", "вечеринка"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("events"));
    }

    @Test
    @DisplayName("GET /events/new должен показывать форму создания")
    void newForm_shouldReturnFormView() throws Exception {
        mockMvc.perform(get("/events/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("events/form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attributeExists("types"));
    }

    @Test
    @DisplayName("GET / должен показывать главную страницу")
    void home_shouldReturnIndexView() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }
}