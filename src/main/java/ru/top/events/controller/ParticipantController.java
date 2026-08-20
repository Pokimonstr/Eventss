package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.model.EventParticipant;
import ru.top.events.service.ParticipantService;

@Controller
@RequestMapping("/events/{eventId}/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping
    public String add(@PathVariable Long eventId,
                      @RequestParam String displayName,
                      RedirectAttributes redirectAttributes) {
        try {
            participantService.add(eventId, displayName);
            redirectAttributes.addFlashAttribute("message", "Участник добавлен 🙌");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/{participantId}/rsvp")
    public String rsvp(@PathVariable Long eventId,
                       @PathVariable Long participantId,
                       @RequestParam EventParticipant.RsvpStatus status,
                       RedirectAttributes redirectAttributes) {
        try {
            participantService.setRsvp(eventId, participantId, status);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId;
    }

    @PostMapping("/{participantId}/delete")
    public String remove(@PathVariable Long eventId,
                         @PathVariable Long participantId,
                         RedirectAttributes redirectAttributes) {
        try {
            participantService.remove(eventId, participantId);
            redirectAttributes.addFlashAttribute("message", "Участник удалён");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId;
    }
}
