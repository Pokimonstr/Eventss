package ru.top.events.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Геолокация: расстояние по формуле гаверсинуса")
class LocationDistanceTest {

    @Test
    @DisplayName("Расстояние Москва — Санкт-Петербург около 630 км")
    void calculatesLongDistance() {
        double km = LocationService.distanceKm(55.751244, 37.618423, 59.938784, 30.314997);

        assertThat(km).isBetween(600.0, 660.0);
    }

    @Test
    @DisplayName("Одна и та же точка — нулевое расстояние")
    void zeroForSamePoint() {
        assertThat(LocationService.distanceKm(55.75, 37.61, 55.75, 37.61)).isZero();
    }
}
