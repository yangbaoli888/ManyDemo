package com.example.newborntracker.model;

import jakarta.validation.constraints.NotNull;

public record QuickRecordRequest(
        @NotNull EventType type
) {
}
