package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.top.events.repository.EventParticipantRepository;
import ru.top.events.repository.EventRepository;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final EventRepository eventRepository;
    private final EventParticipantRepository participantRepository;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("eventCount", eventRepository.count());
        model.addAttribute("participantCount", participantRepository.count());
        return "index";
    }
}
