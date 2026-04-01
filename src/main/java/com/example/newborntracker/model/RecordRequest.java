package com.example.newborntracker.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RecordRequest(
        @NotNull EventType type,
        @Min(1) Integer amountMl,
        @Min(1) Integer durationMinutes
) {
}
