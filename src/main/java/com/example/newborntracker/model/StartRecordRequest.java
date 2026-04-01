package com.example.newborntracker.model;

import jakarta.validation.constraints.NotNull;

public record StartRecordRequest(
        @NotNull EventType type
) {
}
