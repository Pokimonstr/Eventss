package ru.top.events.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import ru.top.events.model.EventStatus;
import ru.top.events.model.EventType;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class EventView {
    private Long id;
    private String title;
    private String description;
    private EventType type;
    private EventStatus status;
    private String startAt;
    private String endAt;
    private String locationName;
    private Double latitude;
    private Double longitude;
    private Integer maxParticipants;
    private BigDecimal budgetAmount;
    private String ownerName;
    private int participantCount;
}
