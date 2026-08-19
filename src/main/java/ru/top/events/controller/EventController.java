package ru.top.events.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.dto.EventFormDto;
import ru.top.events.dto.EventView;
import ru.top.events.model.EventParticipant;
import ru.top.events.model.EventStatus;
import ru.top.events.model.EventType;
import ru.top.events.service.AlbumService;
import ru.top.events.service.AssistantService;
import ru.top.events.service.ChatService;
import ru.top.events.service.EventService;
import ru.top.events.service.ExpenseService;
import ru.top.events.service.ParticipantService;

@Slf4j
@Controller
@RequestMapping("/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final ParticipantService participantService;
    private final ExpenseService expenseService;
    private final ChatService chatService;
    private final AlbumService albumService;
    private final AssistantService assistantService;

    @GetMapping
    public String list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(defaultValue = "0") int page,
            Model model) {

        Pageable pageable = PageRequest.of(page, 6);
        Page<EventView> eventsPage = eventService.findAll(search, type, status, pageable);

        boolean hasFilters = (search != null && !search.isBlank()) || type != null || status != null;

        model.addAttribute("events", eventsPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", eventsPage.getTotalPages());
        model.addAttribute("search", search);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("hasFilters", hasFilters);
        model.addAttribute("types", EventType.values());
        model.addAttribute("statuses", EventStatus.values());

        return "events/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("form", new EventFormDto());
        model.addAttribute("types", EventType.values());
        return "events/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") EventFormDto form,
                         BindingResult result, Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("types", EventType.values());
            return "events/form";
        }
        eventService.create(form);
        redirectAttributes.addFlashAttribute("message", "Мероприятие создано! 🎉");
        return "redirect:/events";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("event", eventService.findById(id));
        model.addAttribute("participants", participantService.list(id));
        model.addAttribute("rsvpStatuses", EventParticipant.RsvpStatus.values());
        model.addAttribute("expensesTotal", expenseService.total(id));
        model.addAttribute("chatCount", chatService.count(id));
        model.addAttribute("photoCount", albumService.count(id));
        model.addAttribute("checklistProgress", assistantService.progressPercent(id));
        model.addAttribute("activeTab", "overview");
        return "events/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("form", eventService.toFormDto(id));
        model.addAttribute("types", EventType.values());
        return "events/form";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") EventFormDto form,
                         BindingResult result, Model model,
                         RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("types", EventType.values());
            return "events/form";
        }
        eventService.update(id, form);
        redirectAttributes.addFlashAttribute("message", "Изменения сохранены ✅");
        return "redirect:/events/" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        eventService.delete(id);
        redirectAttributes.addFlashAttribute("message", "Мероприятие удалено");
        return "redirect:/events";
    }
}
