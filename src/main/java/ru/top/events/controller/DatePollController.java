package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.model.VoteChoice;
import ru.top.events.service.CurrentUserService;
import ru.top.events.service.DatePollService;
import ru.top.events.service.EventService;
import ru.top.events.service.ParticipantService;

import java.time.LocalDateTime;

@Controller
@RequestMapping("/events/{eventId}/dates")
@RequiredArgsConstructor
public class DatePollController {

    private final DatePollService datePollService;
    private final EventService eventService;
    private final ParticipantService participantService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("options", datePollService.results(eventId));
        model.addAttribute("participants", participantService.list(eventId));
        model.addAttribute("choices", VoteChoice.values());
        model.addAttribute("activeTab", "dates");
        return "events/dates";
    }

    @PostMapping
    public String addOption(@PathVariable Long eventId,
                            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startAt,
                            @RequestParam(required = false)
                            @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endAt,
                            RedirectAttributes redirectAttributes) {
        try {
            datePollService.addOption(eventId, startAt, endAt);
            redirectAttributes.addFlashAttribute("message", "Вариант даты добавлен 🗓️");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/dates";
    }

    @PostMapping("/{optionId}/vote")
    public String vote(@PathVariable Long eventId,
                       @PathVariable Long optionId,
                       @RequestParam VoteChoice choice,
                       RedirectAttributes redirectAttributes) {
        try {
            datePollService.vote(eventId, optionId, currentUserService.currentId(), choice);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/dates";
    }

    @PostMapping("/{optionId}/confirm")
    public String confirm(@PathVariable Long eventId,
                          @PathVariable Long optionId,
                          RedirectAttributes redirectAttributes) {
        try {
            datePollService.confirm(eventId, optionId);
            redirectAttributes.addFlashAttribute("message", "Дата зафиксирована ✅");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/{optionId}/delete")
    public String delete(@PathVariable Long eventId,
                         @PathVariable Long optionId,
                         RedirectAttributes redirectAttributes) {
        try {
            datePollService.removeOption(eventId, optionId);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/dates";
    }
}
