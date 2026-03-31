package com.example.newborntracker.model;

import java.time.LocalDate;

public record DailyStats(
        LocalDate date,
        int stoolCount,
        int urineCount,
        int formulaTotalMl,
        int breastfeedingTotalMinutes,
        int breastfeedingTotalMl,
        int weightCount,
        Integer latestWeightGrams
) {
}
