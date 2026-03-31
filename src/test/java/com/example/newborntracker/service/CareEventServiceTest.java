package com.example.newborntracker.service;

import com.example.newborntracker.model.EventType;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CareEventServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void formulaMustBeMultipleOfThirty() {
        Path file = tempDir.resolve("events.json");
        CareEventService service = new CareEventService(new ObjectMapper(), file.toString());
        service.init();

        service.start(EventType.FORMULA);
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.end(EventType.FORMULA, 100)
        );

        assertEquals("奶粉毫升数必须为30的倍数", ex.getMessage());
    }

    @Test
    void recordsShouldBeLoadedAfterRestart() {
        Path file = tempDir.resolve("events.json");

        CareEventService first = new CareEventService(new ObjectMapper(), file.toString());
        first.init();
        first.quickRecord(EventType.URINE);

        CareEventService second = new CareEventService(new ObjectMapper(), file.toString());
        second.init();

        assertEquals(1, second.all().size());
        assertEquals(EventType.URINE, second.all().get(0).type());
    }

    @Test
    void startAndEndFormulaShouldMergeIntoSingleRecord() {
        Path file = tempDir.resolve("events.json");
        CareEventService service = new CareEventService(new ObjectMapper(), file.toString());
        service.init();

        service.start(EventType.FORMULA);
        var event = service.end(EventType.FORMULA, 120);

        assertEquals(EventType.FORMULA, event.type());
        assertEquals(120, event.amountMl());
        assertEquals(1, service.all().size());
    }
}
