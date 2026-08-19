package ru.top.events.repository;

import org.springframework.data.jpa.domain.Specification;
import ru.top.events.model.Event;
import ru.top.events.model.EventStatus;
import ru.top.events.model.EventType;

public class EventSpecifications {

    public static Specification<Event> withFilters(String search, EventType type, EventStatus status) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.trim().isEmpty()) {
                predicates.add(
                        cb.like(cb.lower(root.get("title")), "%" + search.toLowerCase() + "%")
                );
            }

            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            query.orderBy(cb.desc(root.get("createdAt")));

            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
    }
}