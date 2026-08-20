package ru.top.events.service;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.model.User;
import ru.top.events.repository.UserRepository;

import java.util.List;

/**
 * В проекте пока нет авторизации, поэтому «текущий пользователь» хранится в сессии.
 * Это позволяет голосовать, писать в чат и делить счета от лица разных участников.
 */
@Service
@RequiredArgsConstructor
public class CurrentUserService {

    static final String SESSION_KEY = "currentUserId";

    private final UserRepository userRepository;
    private final HttpSession session;

    @Transactional(readOnly = true)
    public User current() {
        Long id = (Long) session.getAttribute(SESSION_KEY);
        if (id != null) {
            return userRepository.findById(id).orElseGet(this::fallback);
        }
        return fallback();
    }

    public Long currentId() {
        return current().getId();
    }

    @Transactional(readOnly = true)
    public List<User> allUsers() {
        return userRepository.findAll(org.springframework.data.domain.Sort.by("id"));
    }

    public void switchTo(Long userId) {
        userRepository.findById(userId)
                .ifPresent(user -> session.setAttribute(SESSION_KEY, user.getId()));
    }

    private User fallback() {
        return userRepository.findAll(org.springframework.data.domain.Sort.by("id")).stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("В базе нет ни одного пользователя"));
    }
}
