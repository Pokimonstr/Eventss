package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.service.AssistantService;
import ru.top.events.service.EventService;

@Controller
@RequestMapping("/events/{eventId}/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final EventService eventService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("checklist", assistantService.checklist(eventId));
        model.addAttribute("progress", assistantService.progressPercent(eventId));
        model.addAttribute("suggestions", assistantService.suggestLocations(eventId));
        model.addAttribute("advice", assistantService.advice(eventId));
        model.addAttribute("activeTab", "assistant");
        return "events/assistant";
    }

    @PostMapping("/generate")
    public String generate(@PathVariable Long eventId, RedirectAttributes redirectAttributes) {
        assistantService.generateChecklist(eventId);
        redirectAttributes.addFlashAttribute("message", "Чек-лист собран 🤖");
        return "redirect:/events/" + eventId + "/assistant";
    }

    @PostMapping("/items")
    public String addItem(@PathVariable Long eventId,
                          @RequestParam String text,
                          RedirectAttributes redirectAttributes) {
        try {
            assistantService.addItem(eventId, text);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/assistant";
    }

    @PostMapping("/items/{itemId}/toggle")
    public String toggle(@PathVariable Long eventId, @PathVariable Long itemId) {
        assistantService.toggleItem(eventId, itemId);
        return "redirect:/events/" + eventId + "/assistant";
    }

    @PostMapping("/items/{itemId}/delete")
    public String delete(@PathVariable Long eventId, @PathVariable Long itemId) {
        assistantService.deleteItem(eventId, itemId);
        return "redirect:/events/" + eventId + "/assistant";
    }
}
