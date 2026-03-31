package com.example.newborntracker.model;

import java.time.Instant;

public record CareEvent(
        long id,
        EventType type,
        Instant happenedAt,
        Instant startedAt,
        Instant endedAt,
        Integer amountMl,
        Integer durationMinutes,
        Integer weightGrams
) {
}
