package com.example.newborntracker.model;

import java.time.LocalDate;
import java.util.List;

public record TimelineDay(
        LocalDate date,
        List<CareEvent> events
) {
}
