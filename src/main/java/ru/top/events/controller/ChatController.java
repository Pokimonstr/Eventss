package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import ru.top.events.dto.ChatMessageView;
import ru.top.events.service.ChatService;
import ru.top.events.service.CurrentUserService;
import ru.top.events.service.EventService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/events/{eventId}/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;
    private final EventService eventService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("messages", chatService.history(eventId, currentUserService.currentId()));
        model.addAttribute("activeTab", "chat");
        return "events/chat";
    }

    @GetMapping(value = "/messages", produces = "application/json")
    @ResponseBody
    public List<ChatMessageView> messages(@PathVariable Long eventId,
                                          @RequestParam(required = false) Long afterId) {
        return chatService.since(eventId, afterId, currentUserService.currentId());
    }

    @PostMapping(value = "/messages", produces = "application/json")
    @ResponseBody
    public ChatMessageView send(@PathVariable Long eventId, @RequestBody Map<String, String> body) {
        return chatService.post(eventId, currentUserService.currentId(), body.get("text"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseBody
    public ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(Map.of("error", ex.getMessage()));
    }
}
