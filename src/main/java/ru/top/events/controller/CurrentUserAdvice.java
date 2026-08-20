package ru.top.events.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.top.events.model.User;
import ru.top.events.service.CurrentUserService;

import java.util.List;

/**
 * Кладёт «текущего пользователя» в модель всех страниц — от его лица
 * идут голоса, сообщения в чате, расходы и геопозиция.
 */
@ControllerAdvice
@RequiredArgsConstructor
public class CurrentUserAdvice {

    private final CurrentUserService currentUserService;

    @ModelAttribute("currentUser")
    public User currentUser() {
        try {
            return currentUserService.current();
        } catch (IllegalStateException ex) {
            return null;
        }
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("allUsers")
    public List<User> allUsers() {
        return currentUserService.allUsers();
    }
}
