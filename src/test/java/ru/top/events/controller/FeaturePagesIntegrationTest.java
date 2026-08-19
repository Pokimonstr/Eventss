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
import ru.top.events.model.EventStatus;
import ru.top.events.model.EventType;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.UserRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("Страницы новых функций мероприятия")
class FeaturePagesIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private UserRepository userRepository;

    private Long eventId;

    @BeforeEach
    void setUp() {
        eventRepository.deleteAll();
        userRepository.deleteAll();

        User owner = userRepository.save(User.builder()
                .username("anya")
                .email("owner@letsgo.ru")
                .password("secret")
                .displayName("Аня")
                .build());

        Event event = eventRepository.save(Event.builder()
                .title("Поход в горы")
                .description("Тестовое мероприятие")
                .type(EventType.HIKE)
                .status(EventStatus.PLANNING)
                .owner(owner)
                .startAt(LocalDateTime.now().plusDays(7))
                .budgetAmount(new BigDecimal("5000.00"))
                .build());
        eventId = event.getId();
    }

    @Test
    @DisplayName("Все вкладки мероприятия отдают 200 и рендерятся")
    void rendersAllTabs() throws Exception {
        mockMvc.perform(get("/events/" + eventId)).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Альбом")));
        mockMvc.perform(get("/events/" + eventId + "/dates")).andExpect(status().isOk());
        mockMvc.perform(get("/events/" + eventId + "/expenses")).andExpect(status().isOk());
        mockMvc.perform(get("/events/" + eventId + "/chat")).andExpect(status().isOk());
        mockMvc.perform(get("/events/" + eventId + "/location")).andExpect(status().isOk());
        mockMvc.perform(get("/events/" + eventId + "/assistant")).andExpect(status().isOk());
        mockMvc.perform(get("/events/" + eventId + "/album")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Ассистент генерирует чек-лист")
    void generatesChecklist() throws Exception {
        mockMvc.perform(post("/events/" + eventId + "/assistant/generate"))
                .andExpect(redirectedUrl("/events/" + eventId + "/assistant"));

        mockMvc.perform(get("/events/" + eventId + "/assistant"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("check-row")));
    }

    @Test
    @DisplayName("Вариант даты добавляется и появляется в голосовании")
    void addsDateOption() throws Exception {
        String start = LocalDateTime.now().plusDays(3).withNano(0).withSecond(0).toString().substring(0, 16);

        mockMvc.perform(post("/events/" + eventId + "/dates").param("startAt", start))
                .andExpect(redirectedUrl("/events/" + eventId + "/dates"));

        mockMvc.perform(get("/events/" + eventId + "/dates"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("option-card")));
    }

    @Test
    @DisplayName("Сообщение чата сохраняется и возвращается в истории")
    void postsChatMessage() throws Exception {
        mockMvc.perform(post("/events/" + eventId + "/chat/messages")
                        .contentType("application/json")
                        .content("{\"text\":\"Всем привет!\"}".getBytes(StandardCharsets.UTF_8)))
                .andExpect(status().isOk());

        String history = mockMvc.perform(get("/events/" + eventId + "/chat/messages"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(history).contains("Всем привет!");
    }
}
