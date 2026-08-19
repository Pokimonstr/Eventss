package ru.top.events.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.top.events.service.CurrentUserService;

@Controller
@RequiredArgsConstructor
public class UserSwitchController {

    private final CurrentUserService currentUserService;

    @PostMapping("/switch-user")
    public String switchUser(@RequestParam Long userId,
                             @RequestParam(defaultValue = "/") String redirect) {
        currentUserService.switchTo(userId);
        return "redirect:" + (redirect.startsWith("/") ? redirect : "/");
    }
}
