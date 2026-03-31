package com.example.newborntracker.service;

import com.example.newborntracker.model.EventType;
import com.example.newborntracker.model.RecordRequest;
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

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.record(new RecordRequest(EventType.FORMULA, 100, null))
        );

        assertEquals("奶粉毫升数必须为30的倍数", ex.getMessage());
    }

    @Test
    void recordsShouldBeLoadedAfterRestart() {
        Path file = tempDir.resolve("events.json");

        CareEventService first = new CareEventService(new ObjectMapper(), file.toString());
        first.init();
        first.record(new RecordRequest(EventType.URINE, null, null));

        CareEventService second = new CareEventService(new ObjectMapper(), file.toString());
        second.init();

        assertEquals(1, second.all().size());
        assertEquals(EventType.URINE, second.all().get(0).type());
    }
}
