package ru.top.events.exception;

public class EventNotFoundException extends RuntimeException {
    public EventNotFoundException(Long id) {
        super("Мероприятие с id=" + id + " не найдено");
    }
}