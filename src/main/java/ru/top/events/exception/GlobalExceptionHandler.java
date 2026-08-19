package ru.top.events.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EventNotFoundException.class)
    public String handleNotFound(EventNotFoundException ex, Model model) {
        log.warn(ex.getMessage());
        model.addAttribute("error", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        log.error("Непредвиденная ошибка", ex);
        model.addAttribute("error", "Что-то пошло не так :(");
        return "error";
    }
}