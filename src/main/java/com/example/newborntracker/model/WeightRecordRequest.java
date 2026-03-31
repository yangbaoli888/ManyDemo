package com.example.newborntracker.model;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record WeightRecordRequest(
        @NotNull @Min(1) Integer weightGrams
) {
}
