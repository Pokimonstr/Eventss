package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.LocationSuggestion;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.ChecklistItem;
import ru.top.events.model.Event;
import ru.top.events.model.EventType;
import ru.top.events.repository.ChecklistItemRepository;
import ru.top.events.repository.EventParticipantRepository;
import ru.top.events.repository.EventRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * AI-ассистент: собирает чек-лист под тип мероприятия и подбирает локации
 * по бюджету на человека. Работает по набору правил, без внешних сервисов.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssistantService {

    private static final Map<EventType, List<String>> CHECKLIST_TEMPLATES = Map.ofEntries(
            Map.entry(EventType.PARTY, List.of(
                    "Согласовать дату со всеми участниками",
                    "Забронировать место или подготовить квартиру",
                    "Составить плейлист",
                    "Купить напитки и снеки",
                    "Продумать закуски для тех, кто не пьёт",
                    "Подготовить колонку и свет",
                    "Договориться, кто убирается после")),
            Map.entry(EventType.HIKE, List.of(
                    "Проверить прогноз погоды",
                    "Построить маршрут и скинуть его всем",
                    "Собрать аптечку",
                    "Взять воду из расчёта 1,5 л на человека",
                    "Проверить обувь и одежду по погоде",
                    "Зарядить пауэрбанк и телефоны",
                    "Назначить ответственного за еду")),
            Map.entry(EventType.MOVIE, List.of(
                    "Выбрать фильм голосованием",
                    "Купить билеты заранее",
                    "Согласовать время сеанса",
                    "Договориться о встрече за 20 минут до начала",
                    "Решить, кто берёт попкорн")),
            Map.entry(EventType.BIRTHDAY, List.of(
                    "Составить список гостей",
                    "Скинуться на подарок",
                    "Заказать торт",
                    "Забронировать место",
                    "Подготовить украшения и свечи",
                    "Продумать поздравление и тосты",
                    "Назначить фотографа среди своих")),
            Map.entry(EventType.DINNER, List.of(
                    "Выбрать ресторан или меню",
                    "Забронировать столик",
                    "Уточнить аллергии и предпочтения",
                    "Согласовать бюджет на человека",
                    "Договориться, как делим счёт")),
            Map.entry(EventType.GAME, List.of(
                    "Выбрать игры",
                    "Проверить, что хватает контроллеров/фишек",
                    "Подготовить снеки",
                    "Определить регламент и таймер",
                    "Придумать приз победителю")),
            Map.entry(EventType.CONCERT, List.of(
                    "Купить билеты",
                    "Проверить правила площадки",
                    "Договориться о точке встречи у входа",
                    "Продумать, как добираться обратно",
                    "Зарядить телефоны для фото")),
            Map.entry(EventType.SPORT, List.of(
                    "Забронировать площадку или корт",
                    "Проверить инвентарь",
                    "Взять воду и полотенца",
                    "Собрать состав команд",
                    "Проверить страховку и аптечку")),
            Map.entry(EventType.OTHER, List.of(
                    "Определить цель встречи",
                    "Согласовать дату и время",
                    "Выбрать место",
                    "Составить список участников",
                    "Определить бюджет"))
    );

    private static final Map<EventType, List<LocationSuggestion>> LOCATION_CATALOG = Map.ofEntries(
            Map.entry(EventType.PARTY, List.of(
                    new LocationSuggestion("Квартира кого-то из своих", "Бесплатно и без ограничений по времени", "почти даром", 300),
                    new LocationSuggestion("Антикафе", "Оплата по времени, настолки и кухня включены", "средний", 800),
                    new LocationSuggestion("Лофт с террасой", "Место под большую компанию и музыку", "выше среднего", 2500))),
            Map.entry(EventType.HIKE, List.of(
                    new LocationSuggestion("Городской лесопарк", "Лёгкий маршрут на полдня, добраться на транспорте", "почти даром", 200),
                    new LocationSuggestion("Загородная тропа у реки", "Маршрут 10–15 км с местом для привала", "средний", 700),
                    new LocationSuggestion("Заповедник с гидом", "Организованный маршрут и трансфер", "выше среднего", 2000))),
            Map.entry(EventType.MOVIE, List.of(
                    new LocationSuggestion("Домашний кинотеатр", "Проектор, плед и своя еда", "почти даром", 250),
                    new LocationSuggestion("Сетевой кинотеатр", "Утренние сеансы дешевле вечерних", "средний", 600),
                    new LocationSuggestion("Кинотеатр с диванами", "Отдельный зал под компанию", "выше среднего", 1800))),
            Map.entry(EventType.BIRTHDAY, List.of(
                    new LocationSuggestion("Кафе с отдельным залом", "Депозит вместо аренды", "средний", 1500),
                    new LocationSuggestion("Пикник в парке", "Своя еда, аренда беседки", "почти даром", 500),
                    new LocationSuggestion("Банкетный зал", "Меню, украшение и ведущий", "выше среднего", 3500))),
            Map.entry(EventType.DINNER, List.of(
                    new LocationSuggestion("Столовая-бистро", "Быстро и дёшево, подходит для будней", "почти даром", 400),
                    new LocationSuggestion("Семейный ресторан", "Средний чек и большие порции", "средний", 1200),
                    new LocationSuggestion("Ресторан авторской кухни", "Для особого повода", "выше среднего", 3000))),
            Map.entry(EventType.GAME, List.of(
                    new LocationSuggestion("Дома у организатора", "Свои настолки и приставка", "почти даром", 200),
                    new LocationSuggestion("Игровое антикафе", "Большая библиотека игр", "средний", 700),
                    new LocationSuggestion("Компьютерный клуб", "Аренда зала на компанию", "выше среднего", 1500))),
            Map.entry(EventType.CONCERT, List.of(
                    new LocationSuggestion("Локальный клуб", "Молодые группы, дешёвые билеты", "почти даром", 800),
                    new LocationSuggestion("Городская площадка", "Средние билеты, удобный вход", "средний", 2000),
                    new LocationSuggestion("Большая арена", "Крупные туры, брать билеты заранее", "выше среднего", 5000))),
            Map.entry(EventType.SPORT, List.of(
                    new LocationSuggestion("Школьная или дворовая площадка", "Бесплатно, свой инвентарь", "почти даром", 100),
                    new LocationSuggestion("Аренда корта на час", "Делится на всех участников", "средний", 600),
                    new LocationSuggestion("Спортивный комплекс", "Раздевалки, душ, инвентарь", "выше среднего", 1500))),
            Map.entry(EventType.OTHER, List.of(
                    new LocationSuggestion("Парк или набережная", "Бесплатно и просто договориться", "почти даром", 100),
                    new LocationSuggestion("Кофейня", "Тихое место для разговора", "средний", 500),
                    new LocationSuggestion("Коворкинг с переговоркой", "Если нужна техника и тишина", "выше среднего", 1200)))
    );

    private final ChecklistItemRepository checklistRepository;
    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public List<ChecklistItem> checklist(Long eventId) {
        return checklistRepository.findByEventIdOrderBySortOrderAscIdAsc(eventId);
    }

    @Transactional(readOnly = true)
    public int progressPercent(Long eventId) {
        long total = checklistRepository.countByEventId(eventId);
        if (total == 0) return 0;
        return (int) Math.round(checklistRepository.countByEventIdAndDoneTrue(eventId) * 100.0 / total);
    }

    /**
     * Перегенерирует автоматические пункты чек-листа, ручные пункты сохраняются.
     */
    @Transactional
    public List<ChecklistItem> generateChecklist(Long eventId) {
        Event event = getEvent(eventId);
        checklistRepository.deleteByEventIdAndGeneratedTrue(eventId);

        List<String> base = new ArrayList<>(CHECKLIST_TEMPLATES.getOrDefault(event.getType(),
                CHECKLIST_TEMPLATES.get(EventType.OTHER)));
        base.addAll(contextualItems(event));

        List<ChecklistItem> items = new ArrayList<>();
        int order = 0;
        for (String text : base) {
            items.add(ChecklistItem.builder()
                    .event(event)
                    .text(text)
                    .category(event.getType().getLabel())
                    .generated(true)
                    .sortOrder(order++)
                    .build());
        }
        checklistRepository.saveAll(items);
        log.info("Ассистент собрал {} пунктов чек-листа для мероприятия id={}", items.size(), eventId);
        return checklist(eventId);
    }

    @Transactional
    public void addItem(Long eventId, String text) {
        Event event = getEvent(eventId);
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("Пункт не может быть пустым");
        }
        checklistRepository.save(ChecklistItem.builder()
                .event(event)
                .text(text.trim())
                .category("Своё")
                .generated(false)
                .sortOrder(1000 + (int) checklistRepository.countByEventId(eventId))
                .build());
    }

    @Transactional
    public void toggleItem(Long eventId, Long itemId) {
        ChecklistItem item = getItem(eventId, itemId);
        item.setDone(!item.isDone());
        checklistRepository.save(item);
    }

    @Transactional
    public void deleteItem(Long eventId, Long itemId) {
        checklistRepository.delete(getItem(eventId, itemId));
    }

    /**
     * Подбор локаций: сначала те, что укладываются в бюджет на человека.
     */
    @Transactional(readOnly = true)
    public List<LocationSuggestion> suggestLocations(Long eventId) {
        Event event = getEvent(eventId);
        List<LocationSuggestion> catalog = LOCATION_CATALOG.getOrDefault(event.getType(),
                LOCATION_CATALOG.get(EventType.OTHER));

        BigDecimal perPerson = budgetPerPerson(event);
        if (perPerson == null) {
            return catalog;
        }
        int limit = perPerson.intValue();
        List<LocationSuggestion> fits = catalog.stream().filter(s -> s.pricePerPerson() <= limit).toList();
        List<LocationSuggestion> rest = catalog.stream().filter(s -> s.pricePerPerson() > limit).toList();

        List<LocationSuggestion> result = new ArrayList<>(fits);
        result.addAll(rest);
        return result;
    }

    /**
     * Короткие советы по конкретному мероприятию.
     */
    @Transactional(readOnly = true)
    public List<String> advice(Long eventId) {
        Event event = getEvent(eventId);
        List<String> tips = new ArrayList<>();

        long participants = participantRepository.findByEventId(eventId).size();
        BigDecimal perPerson = budgetPerPerson(event);

        if (event.getStartAt() == null) {
            tips.add("Дата ещё не выбрана — запусти голосование во вкладке «Даты», алгоритм найдёт время, когда могут все.");
        } else {
            long days = Duration.between(LocalDateTime.now(), event.getStartAt()).toDays();
            if (days < 0) {
                tips.add("Мероприятие уже прошло — загрузи фото в альбом воспоминаний.");
            } else if (days <= 2) {
                tips.add("До встречи меньше двух дней: напомни всем время и точку сбора в чате.");
            } else {
                tips.add("До встречи " + days + " дн. — самое время закрыть пункты чек-листа.");
            }
        }

        if (event.getLatitude() == null || event.getLongitude() == null) {
            tips.add("Отметь точку встречи на карте, чтобы все видели, куда идти, и делились live-статусом.");
        }

        if (participants <= 1) {
            tips.add("Пока в мероприятии один участник — добавь друзей, тогда заработают голосование и деление счёта.");
        }

        if (perPerson != null) {
            tips.add("Бюджет примерно " + perPerson + " ₽ на человека — ориентируйся на него при выборе локации.");
        } else {
            tips.add("Укажи бюджет мероприятия, и я подберу локации под сумму на человека.");
        }

        if (event.getMaxParticipants() != null && participants >= event.getMaxParticipants()) {
            tips.add("Достигнут лимит участников — новых добавляй в лист ожидания.");
        }

        return tips;
    }

    private List<String> contextualItems(Event event) {
        List<String> items = new ArrayList<>();
        if (event.getStartAt() == null) {
            items.add("Выбрать дату голосованием участников");
        }
        if (event.getLocationName() == null || event.getLocationName().isBlank()) {
            items.add("Определить место встречи и отметить его на карте");
        }
        if (event.getBudgetAmount() != null) {
            items.add("Собрать деньги и внести расходы во вкладку «Счета»");
        }
        items.add("Создать чат и позвать всех участников");
        return items;
    }

    private BigDecimal budgetPerPerson(Event event) {
        if (event.getBudgetAmount() == null) return null;
        long participants = Math.max(1, participantRepository.findByEventId(event.getId()).size());
        return event.getBudgetAmount().divide(BigDecimal.valueOf(participants), 0, RoundingMode.HALF_UP);
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }

    private ChecklistItem getItem(Long eventId, Long itemId) {
        ChecklistItem item = checklistRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Пункт не найден"));
        if (!item.getEvent().getId().equals(eventId)) {
            throw new IllegalArgumentException("Пункт относится к другому мероприятию");
        }
        return item;
    }
}
