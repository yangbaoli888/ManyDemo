package com.example.newborntracker.model;

import java.time.Instant;

public record CareEvent(
        long id,
        EventType type,
        Instant happenedAt,
        Integer amountMl,
        Integer durationMinutes
) {
}
