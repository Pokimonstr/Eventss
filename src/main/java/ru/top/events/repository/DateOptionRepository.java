package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.DateOption;

import java.util.List;

public interface DateOptionRepository extends JpaRepository<DateOption, Long> {
    List<DateOption> findByEventIdOrderByStartAtAsc(Long eventId);
}
