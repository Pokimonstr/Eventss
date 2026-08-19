package ru.top.events.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.top.events.dto.ParticipantLocationView;
import ru.top.events.exception.EventNotFoundException;
import ru.top.events.model.Event;
import ru.top.events.model.ParticipantLocation;
import ru.top.events.model.TravelStatus;
import ru.top.events.model.User;
import ru.top.events.repository.EventRepository;
import ru.top.events.repository.ParticipantLocationRepository;
import ru.top.events.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Геолокация: точка встречи на карте и live-статусы участников.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocationService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final double EARTH_RADIUS_KM = 6371.0;
    private static final double CITY_SPEED_KMH = 15.0;
    private static final double ARRIVED_KM = 0.15;
    private static final double NEARBY_KM = 1.0;

    private final ParticipantLocationRepository locationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;

    @Transactional
    public void setMeetingPoint(Long eventId, Double latitude, Double longitude, String name) {
        Event event = getEvent(eventId);
        validateCoordinates(latitude, longitude);
        event.setLatitude(latitude);
        event.setLongitude(longitude);
        if (name != null && !name.isBlank()) {
            event.setLocationName(name.trim());
        }
        eventRepository.save(event);
        log.info("Точка встречи мероприятия id={} обновлена: {}, {}", eventId, latitude, longitude);
    }

    @Transactional
    public ParticipantLocationView updateMyLocation(Long eventId, Long userId, Double latitude, Double longitude) {
        Event event = getEvent(eventId);
        validateCoordinates(latitude, longitude);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        ParticipantLocation location = locationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseGet(() -> ParticipantLocation.builder().event(event).user(user).build());
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        location.setStatus(autoStatus(event, latitude, longitude));

        return toView(locationRepository.save(location), event);
    }

    @Transactional
    public void setStatus(Long eventId, Long userId, TravelStatus status) {
        ParticipantLocation location = locationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Сначала поделись геопозицией"));
        location.setStatus(status);
        locationRepository.save(location);
    }

    @Transactional(readOnly = true)
    public List<ParticipantLocationView> participants(Long eventId) {
        Event event = getEvent(eventId);
        return locationRepository.findByEventId(eventId).stream()
                .map(l -> toView(l, event))
                .sorted(Comparator.comparing(v -> v.distanceKm() == null ? Double.MAX_VALUE : v.distanceKm()))
                .toList();
    }

    /**
     * Расстояние по формуле гаверсинуса, км.
     */
    static double distanceKm(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private TravelStatus autoStatus(Event event, double latitude, double longitude) {
        if (event.getLatitude() == null || event.getLongitude() == null) {
            return TravelStatus.ON_THE_WAY;
        }
        double distance = distanceKm(latitude, longitude, event.getLatitude(), event.getLongitude());
        if (distance <= ARRIVED_KM) return TravelStatus.ARRIVED;
        if (distance <= NEARBY_KM) return TravelStatus.NEARBY;
        return TravelStatus.ON_THE_WAY;
    }

    private ParticipantLocationView toView(ParticipantLocation location, Event event) {
        Double distance = null;
        Integer eta = null;
        if (event.getLatitude() != null && event.getLongitude() != null) {
            distance = Math.round(distanceKm(location.getLatitude(), location.getLongitude(),
                    event.getLatitude(), event.getLongitude()) * 100) / 100.0;
            eta = (int) Math.ceil(distance / CITY_SPEED_KMH * 60);
        }
        TravelStatus status = location.getStatus();
        return new ParticipantLocationView(
                location.getUser().getId(),
                location.getUser().getDisplayName(),
                location.getLatitude(),
                location.getLongitude(),
                status.name(),
                status.getLabel(),
                status.getIcon(),
                distance,
                eta,
                location.getUpdatedAt() == null ? null : location.getUpdatedAt().format(FMT));
    }

    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("Некорректные координаты");
        }
    }

    private Event getEvent(Long eventId) {
        return eventRepository.findById(eventId).orElseThrow(() -> new EventNotFoundException(eventId));
    }
}
