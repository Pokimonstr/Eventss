package ru.top.events.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import ru.top.events.model.EventType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class EventFormDto {

    private Long id;

    @NotBlank(message = "Название обязательно")
    @Size(max = 200, message = "Максимум 200 символов")
    private String title;

    @Size(max = 2000, message = "Слишком длинное описание")
    private String description;

    @NotNull(message = "Выбери тип мероприятия")
    private EventType type;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startAt;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endAt;

    @Size(max = 255)
    private String locationName;

    @Min(value = 1, message = "Минимум 1 участник")
    private Integer maxParticipants;

    @DecimalMin(value = "0.0", message = "Бюджет не может быть отрицательным")
    private BigDecimal budgetAmount;

    @AssertTrue(message = "Дата окончания должна быть позже начала")
    public boolean isEndAfterStart() {
        if (startAt == null || endAt == null) {
            return true;
        }
        return endAt.isAfter(startAt);
    }
}
