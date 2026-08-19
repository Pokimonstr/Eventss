package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.top.events.dto.ParticipantLocationView;
import ru.top.events.model.TravelStatus;
import ru.top.events.service.CurrentUserService;
import ru.top.events.service.EventService;
import ru.top.events.service.LocationService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/events/{eventId}/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;
    private final EventService eventService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("locations", locationService.participants(eventId));
        model.addAttribute("statuses", TravelStatus.values());
        model.addAttribute("activeTab", "location");
        return "events/location";
    }

    @PostMapping("/point")
    public String setPoint(@PathVariable Long eventId,
                           @RequestParam Double latitude,
                           @RequestParam Double longitude,
                           @RequestParam(required = false) String name,
                           RedirectAttributes redirectAttributes) {
        try {
            locationService.setMeetingPoint(eventId, latitude, longitude, name);
            redirectAttributes.addFlashAttribute("message", "Точка встречи сохранена 📍");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/location";
    }

    @PostMapping("/status")
    public String setStatus(@PathVariable Long eventId,
                            @RequestParam TravelStatus status,
                            RedirectAttributes redirectAttributes) {
        try {
            locationService.setStatus(eventId, currentUserService.currentId(), status);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/location";
    }

    @PostMapping(value = "/me", produces = "application/json")
    @ResponseBody
    public ParticipantLocationView updateMyLocation(@PathVariable Long eventId,
                                                    @RequestBody Map<String, Double> body) {
        return locationService.updateMyLocation(eventId, currentUserService.currentId(),
                body.get("latitude"), body.get("longitude"));
    }

    @GetMapping(value = "/participants", produces = "application/json")
    @ResponseBody
    public List<ParticipantLocationView> participants(@PathVariable Long eventId) {
        return locationService.participants(eventId);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
