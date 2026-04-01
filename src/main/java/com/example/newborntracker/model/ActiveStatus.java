package com.example.newborntracker.model;

import java.time.Instant;

public record ActiveStatus(
        Instant formulaStartedAt,
        Instant breastfeedingStartedAt
) {
}
