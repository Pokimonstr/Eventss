package ru.top.events.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.top.events.model.DateVote;

import java.util.List;
import java.util.Optional;

public interface DateVoteRepository extends JpaRepository<DateVote, Long> {
    Optional<DateVote> findByOptionIdAndUserId(Long optionId, Long userId);
    List<DateVote> findByOptionEventId(Long eventId);
}
