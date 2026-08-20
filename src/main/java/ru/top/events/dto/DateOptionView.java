package ru.top.events.dto;

import ru.top.events.model.VoteChoice;

import java.util.List;
import java.util.Map;

/**
 * Вариант даты вместе с результатами голосования.
 */
public record DateOptionView(
        Long id,
        String startAt,
        String endAt,
        int yesCount,
        int maybeCount,
        int noCount,
        int score,
        boolean everyoneCan,
        boolean recommended,
        List<String> yesNames,
        List<String> maybeNames,
        List<String> noNames,
        Map<Long, VoteChoice> votesByUserId
) {
}
