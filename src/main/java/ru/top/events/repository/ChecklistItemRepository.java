package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.ChecklistItem;

import java.util.List;

public interface ChecklistItemRepository extends JpaRepository<ChecklistItem, Long> {
    List<ChecklistItem> findByEventIdOrderBySortOrderAscIdAsc(Long eventId);
    long countByEventId(Long eventId);
    long countByEventIdAndDoneTrue(Long eventId);
    void deleteByEventIdAndGeneratedTrue(Long eventId);
}
