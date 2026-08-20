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
import ru.top.events.service.EventService;
import ru.top.events.service.ExpenseService;
import ru.top.events.service.ParticipantService;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/events/{eventId}/expenses")
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseService expenseService;
    private final EventService eventService;
    private final ParticipantService participantService;

    @GetMapping
    public String page(@PathVariable Long eventId, Model model) {
        model.addAttribute("event", eventService.findById(eventId));
        model.addAttribute("expenses", expenseService.list(eventId));
        model.addAttribute("balances", expenseService.balances(eventId));
        model.addAttribute("settlements", expenseService.settlements(eventId));
        model.addAttribute("total", expenseService.total(eventId));
        model.addAttribute("participants", participantService.list(eventId));
        model.addAttribute("activeTab", "expenses");
        return "events/expenses";
    }

    @PostMapping
    public String add(@PathVariable Long eventId,
                      @RequestParam String title,
                      @RequestParam BigDecimal amount,
                      @RequestParam Long payerId,
                      @RequestParam(required = false) List<Long> sharedWith,
                      RedirectAttributes redirectAttributes) {
        try {
            expenseService.add(eventId, payerId, title, amount, sharedWith);
            redirectAttributes.addFlashAttribute("message", "Расход добавлен 💸");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/expenses";
    }

    @PostMapping("/{expenseId}/delete")
    public String delete(@PathVariable Long eventId,
                         @PathVariable Long expenseId,
                         RedirectAttributes redirectAttributes) {
        try {
            expenseService.delete(eventId, expenseId);
            redirectAttributes.addFlashAttribute("message", "Расход удалён");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/events/" + eventId + "/expenses";
    }
}
